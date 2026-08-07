package com.codesphere.gameon.service;

import com.codesphere.gameon.dao.*;
import com.codesphere.gameon.dto.BrowseFilter;
import com.codesphere.gameon.dto.BrowseListingDto;
import com.codesphere.gameon.dto.ListingDetailDto;
import com.codesphere.gameon.dto.PaginatedResponse;
import com.codesphere.gameon.dto.RosterEntryDto;
import com.codesphere.gameon.exception.ApiException;
import com.codesphere.gameon.model.GameListing;
import com.codesphere.gameon.model.Sport;
import com.codesphere.gameon.model.SportFormat;
import com.codesphere.gameon.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Business logic for browsing game listings (A200).
 * Orchestrates browse queries, applies sport-profile guard, and builds paginated responses.
 */
public class BrowseListingService {

    private static final Logger logger = LoggerFactory.getLogger(BrowseListingService.class);

    private final SportDao sportDao;
    private final GameListingDao gameListingDao;
    private final GameJoinerDao gameJoinerDao;
    private final InvitationDao invitationDao;
    private final SportFormatDao sportFormatDao;
    private final UserDao userDao;

    public BrowseListingService(SportDao sportDao, GameListingDao gameListingDao,
                                GameJoinerDao gameJoinerDao, InvitationDao invitationDao,
                                SportFormatDao sportFormatDao, UserDao userDao) {
        this.sportDao = sportDao;
        this.gameListingDao = gameListingDao;
        this.gameJoinerDao = gameJoinerDao;
        this.invitationDao = invitationDao;
        this.sportFormatDao = sportFormatDao;
        this.userDao = userDao;
    }

    /**
     * Returns a paginated list of browsable listings for the given user.
     * Only returns OPEN, public, future listings for sports on user's profile.
     *
     * @param userId the authenticated user's ID
     * @param filter pagination and optional filter parameters
     * @return paginated response of browse listing DTOs
     */
    public PaginatedResponse<BrowseListingDto> browseListings(long userId, BrowseFilter filter) {
        logger.debug("Browsing listings for userId={}, filter: page={}, size={}, sportId={}, skillLevel={}, date={}, hideFull={}",
                userId, filter.getPage(), filter.getSize(), filter.getSportId(),
                filter.getSkillLevel(), filter.getDate(), filter.isHideFull());

        // 1. Fetch user's sports and extract IDs
        List<Sport> userSports = sportDao.findSportsByUserId(userId);
        List<Long> userSportIds = userSports.stream()
                .map(Sport::getSportId)
                .collect(Collectors.toList());

        // 2. If user has no sports on their profile, return empty response
        if (userSportIds.isEmpty()) {
            logger.debug("User {} has no sports on profile, returning empty results", userId);
            return emptyResponse(filter);
        }

        // 3. If sportId filter is provided but not in user's sport list, return empty response
        if (filter.getSportId() != null && !userSportIds.contains(filter.getSportId())) {
            logger.debug("Sport filter {} not in user's sport list, returning empty results", filter.getSportId());
            return emptyResponse(filter);
        }

        // 4. Query the DAO for the page of results and total count
        List<BrowseListingDto> listings = gameListingDao.findBrowseListings(userSportIds, filter);
        long totalItems = gameListingDao.countBrowseListings(userSportIds, filter);

        // 5. Calculate total pages
        int totalPages = (int) Math.ceil((double) totalItems / filter.getSize());

        logger.debug("Browse query returned {} items on page {}, totalItems={}, totalPages={}",
                listings.size(), filter.getPage(), totalItems, totalPages);

        return new PaginatedResponse<>(listings, filter.getPage(), filter.getSize(), totalItems, totalPages);
    }

    /**
     * Returns full detail for a single listing, including roster.
     * Access control:
     *   - Listing not found: 404
     *   - Creator (game_listing.creator_id = userId): always allowed
     *   - Public listing: user must have the sport on their profile (403 if not)
     *   - Private listing: user must have an invitation record (403 if not)
     *
     * @param userId    the authenticated user's ID
     * @param listingId the listing to retrieve
     * @return full listing detail with roster
     */
    public ListingDetailDto getListingDetail(long userId, long listingId) {
        logger.debug("Fetching listing detail for userId={}, listingId={}", userId, listingId);

        // 1. Find listing by ID → 404 if not found
        GameListing listing = gameListingDao.findById(listingId)
                .orElseThrow(() -> ApiException.notFound("Listing not found"));

        // 2. Access control
        boolean isCreator = listing.getCreatorId() == userId;

        if (!isCreator) {
            // Fetch format to get sportId for access checks
            SportFormat format = sportFormatDao.findById(listing.getFormatId())
                    .orElseThrow(() -> new RuntimeException("Format not found for listing"));

            if (!listing.isPrivate()) {
                // Public listing: check user has sport on profile
                if (!sportDao.userHasSport(userId, format.getSportId())) {
                    throw ApiException.forbidden("You cannot view listings for sports not on your profile");
                }
            } else {
                // Private listing: check invitation record exists
                if (!invitationDao.hasInvitation(listingId, userId)) {
                    throw ApiException.forbidden("Access denied: invitation required");
                }
            }
        }

        // 3. Fetch format info (may already be fetched above, but only in non-creator path)
        SportFormat format = sportFormatDao.findById(listing.getFormatId())
                .orElseThrow(() -> new RuntimeException("Format not found for listing"));

        // 4. Fetch sport name
        Sport sport = sportDao.findById(format.getSportId())
                .orElseThrow(() -> new RuntimeException("Sport not found for format"));

        // 5. Fetch creator username
        User creator = userDao.findById(listing.getCreatorId())
                .orElseThrow(() -> new RuntimeException("Creator user not found"));

        // 6. Fetch spots filled
        int spotsFilled = gameJoinerDao.countAcceptedByListingId(listingId);

        // 7. Fetch roster grouped by team
        Map<String, List<RosterEntryDto>> roster = gameJoinerDao.findRosterByListingId(listingId);

        // 8. Build and return ListingDetailDto
        ListingDetailDto detail = new ListingDetailDto();
        detail.setGameListingId(listing.getGameListingId());
        detail.setSportName(sport.getSportName());
        detail.setFormatName(format.getFormatName());
        detail.setSkillLevel(listing.getSkillLevel());

        // Format date as ISO date string
        LocalDateTime startDt = listing.getDate();
        LocalDateTime endDt = listing.getEndTime();
        detail.setDate(startDt.toLocalDate().toString());

        // Format session window as "HH:mm–HH:mm"
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        String sessionWindow = startDt.format(timeFormatter) + "\u2013" + endDt.format(timeFormatter);
        detail.setSessionWindow(sessionWindow);

        detail.setLocation(listing.getLocation());
        detail.setSpotsFilled(spotsFilled);
        detail.setTotalSpots(format.getNoPlayers());
        detail.setCreatorUsername(creator.getUsername());
        detail.setHasPositions(format.isHasPositions());
        detail.setPrivate(listing.isPrivate());

        // Team A and Team B from roster map
        detail.setTeamA(roster.getOrDefault("A", Collections.emptyList()));
        detail.setTeamB(roster.getOrDefault("B", Collections.emptyList()));

        return detail;
    }

    /**
     * Returns an empty paginated response with the given filter's page and size.
     */
    private PaginatedResponse<BrowseListingDto> emptyResponse(BrowseFilter filter) {
        return new PaginatedResponse<>(Collections.emptyList(), filter.getPage(), filter.getSize(), 0, 0);
    }
}
