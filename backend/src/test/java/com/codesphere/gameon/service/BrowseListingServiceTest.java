package com.codesphere.gameon.service;

import com.codesphere.gameon.dao.*;
import com.codesphere.gameon.dto.*;
import com.codesphere.gameon.exception.ApiException;
import com.codesphere.gameon.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BrowseListingService.
 * Uses fake DAO implementations to avoid database dependency.
 */
class BrowseListingServiceTest {

    private BrowseListingService service;
    private FakeSportDao fakeSportDao;
    private FakeGameListingDao fakeGameListingDao;
    private FakeGameJoinerDao fakeGameJoinerDao;
    private FakeInvitationDao fakeInvitationDao;
    private FakeSportFormatDao fakeSportFormatDao;
    private FakeUserDao fakeUserDao;
    private FakeJoinRequestDao fakeJoinRequestDao;

    private static final long USER_ID = 1L;
    private static final long OTHER_USER_ID = 2L;
    private static final long SPORT_ID = 3L;
    private static final long FORMAT_ID = 7L;
    private static final long LISTING_ID = 100L;

    @BeforeEach
    void setUp() {
        fakeSportDao = new FakeSportDao();
        fakeGameListingDao = new FakeGameListingDao();
        fakeGameJoinerDao = new FakeGameJoinerDao();
        fakeInvitationDao = new FakeInvitationDao();
        fakeSportFormatDao = new FakeSportFormatDao();
        fakeUserDao = new FakeUserDao();
        fakeJoinRequestDao = new FakeJoinRequestDao();

        service = new BrowseListingService(
                fakeSportDao, fakeGameListingDao, fakeGameJoinerDao,
                fakeInvitationDao, fakeSportFormatDao, fakeUserDao, fakeJoinRequestDao);

        // Default setup: user has Basketball on profile, format 3v3
        fakeSportDao.addUserSport(USER_ID, SPORT_ID, "Basketball");
        fakeSportFormatDao.addFormat(new SportFormat(FORMAT_ID, "3v3", false, 6, 60, SPORT_ID));
        fakeUserDao.addUser(new User(OTHER_USER_ID, "creator_user", "pass", "player"));
        fakeUserDao.addUser(new User(USER_ID, "test_user", "pass", "player"));
    }

    // ========================================================
    // browseListings tests
    // ========================================================

    @Test
    void shouldReturnEmptyResultsWhenUserHasNoSports() {
        // User with no sports on profile
        long noSportUserId = 99L;

        BrowseFilter filter = new BrowseFilter();
        PaginatedResponse<BrowseListingDto> response = service.browseListings(noSportUserId, filter);

        assertNotNull(response);
        assertTrue(response.getItems().isEmpty());
        assertEquals(0, response.getTotalItems());
        assertEquals(0, response.getTotalPages());
    }

    @Test
    void shouldReturnEmptyResultsWhenSportIdFilterNotOnProfile() {
        // User has sport 3, but filter requests sport 99
        BrowseFilter filter = new BrowseFilter();
        filter.setSportId(99L);

        PaginatedResponse<BrowseListingDto> response = service.browseListings(USER_ID, filter);

        assertNotNull(response);
        assertTrue(response.getItems().isEmpty());
        assertEquals(0, response.getTotalItems());
        assertEquals(0, response.getTotalPages());
    }

    @Test
    void shouldReturnListingsWhenHideFullIsFalse() {
        // hideFull=false (default) → DAO returns all listings including full ones
        BrowseListingDto listing1 = createBrowseDto(1L, "Basketball", "3v3", 6, 6); // full
        BrowseListingDto listing2 = createBrowseDto(2L, "Basketball", "3v3", 3, 6); // not full
        fakeGameListingDao.setBrowseListings(List.of(listing1, listing2));
        fakeGameListingDao.setBrowseCount(2);

        BrowseFilter filter = new BrowseFilter();
        filter.setHideFull(false);

        PaginatedResponse<BrowseListingDto> response = service.browseListings(USER_ID, filter);

        assertEquals(2, response.getItems().size());
        assertEquals(2, response.getTotalItems());
    }

    @Test
    void shouldExcludeFullListingsWhenHideFullIsTrue() {
        // hideFull=true → DAO only returns non-full listings (DAO handles filtering)
        BrowseListingDto listing1 = createBrowseDto(2L, "Basketball", "3v3", 3, 6); // not full
        fakeGameListingDao.setBrowseListings(List.of(listing1));
        fakeGameListingDao.setBrowseCount(1);

        BrowseFilter filter = new BrowseFilter();
        filter.setHideFull(true);

        PaginatedResponse<BrowseListingDto> response = service.browseListings(USER_ID, filter);

        assertEquals(1, response.getItems().size());
        assertEquals(1, response.getTotalItems());
    }

    @Test
    void shouldUsePaginationDefaults() {
        // Default filter should have page=1, size=20
        fakeGameListingDao.setBrowseListings(List.of());
        fakeGameListingDao.setBrowseCount(0);

        BrowseFilter filter = new BrowseFilter();

        PaginatedResponse<BrowseListingDto> response = service.browseListings(USER_ID, filter);

        assertEquals(1, response.getPage());
        assertEquals(20, response.getSize());
    }

    @Test
    void shouldFilterByDateWhenDateProvided() {
        // Set a date filter → DAO returns matching listings
        LocalDate targetDate = LocalDate.of(2026, 8, 15);
        BrowseListingDto listing = createBrowseDto(1L, "Basketball", "3v3", 2, 6);
        listing.setDate(targetDate.toString());
        fakeGameListingDao.setBrowseListings(List.of(listing));
        fakeGameListingDao.setBrowseCount(1);

        BrowseFilter filter = new BrowseFilter();
        filter.setDate(targetDate);

        PaginatedResponse<BrowseListingDto> response = service.browseListings(USER_ID, filter);

        assertEquals(1, response.getItems().size());
        assertEquals(targetDate.toString(), response.getItems().get(0).getDate());
    }

    // ========================================================
    // getListingDetail tests
    // ========================================================

    @Test
    void shouldReturn404WhenListingNotFound() {
        // findById returns empty → ApiException 404
        ApiException ex = assertThrows(ApiException.class,
                () -> service.getListingDetail(USER_ID, 999L));

        assertEquals(404, ex.getStatus());
        assertTrue(ex.getMessage().contains("Listing not found"));
    }

    @Test
    void shouldReturn403WhenPublicListingSportNotOnProfile() {
        // Public listing, user doesn't have the sport on their profile
        GameListing listing = createPublicListing(LISTING_ID, OTHER_USER_ID, FORMAT_ID);
        fakeGameListingDao.addListing(listing);
        fakeSportFormatDao.addFormat(new SportFormat(FORMAT_ID, "3v3", false, 6, 60, SPORT_ID));

        // Remove the sport from user's profile for this test
        long userWithoutSport = 50L;

        ApiException ex = assertThrows(ApiException.class,
                () -> service.getListingDetail(userWithoutSport, LISTING_ID));

        assertEquals(403, ex.getStatus());
        assertTrue(ex.getMessage().contains("You cannot view listings for sports not on your profile"));
    }

    @Test
    void shouldReturn403WhenPrivateListingWithoutInvitation() {
        // Private listing, requesting user is not the creator and has no invitation
        GameListing listing = createPrivateListing(LISTING_ID, OTHER_USER_ID, FORMAT_ID);
        fakeGameListingDao.addListing(listing);
        fakeSportFormatDao.addFormat(new SportFormat(FORMAT_ID, "3v3", false, 6, 60, SPORT_ID));
        // No invitation added for USER_ID

        ApiException ex = assertThrows(ApiException.class,
                () -> service.getListingDetail(USER_ID, LISTING_ID));

        assertEquals(403, ex.getStatus());
        assertTrue(ex.getMessage().contains("Access denied: invitation required"));
    }

    @Test
    void shouldReturnDetailWhenPrivateListingWithInvitation() {
        // Private listing, not creator, but has an invitation → success
        GameListing listing = createPrivateListing(LISTING_ID, OTHER_USER_ID, FORMAT_ID);
        fakeGameListingDao.addListing(listing);
        fakeSportFormatDao.addFormat(new SportFormat(FORMAT_ID, "3v3", false, 6, 60, SPORT_ID));
        fakeSportDao.addSport(new Sport(SPORT_ID, "Basketball"));
        fakeInvitationDao.addInvitation(LISTING_ID, USER_ID);
        fakeGameJoinerDao.setAcceptedCount(LISTING_ID, 3);
        fakeGameJoinerDao.setRoster(LISTING_ID, Map.of(
                "A", List.of(new RosterEntryDto("player1", null)),
                "B", List.of(new RosterEntryDto("player2", null))
        ));

        ListingDetailDto detail = service.getListingDetail(USER_ID, LISTING_ID);

        assertNotNull(detail);
        assertEquals(LISTING_ID, detail.getGameListingId());
        assertEquals("Basketball", detail.getSportName());
        assertEquals("3v3", detail.getFormatName());
        assertEquals(3, detail.getSpotsFilled());
        assertEquals(6, detail.getTotalSpots());
        assertTrue(detail.isPrivate());
        // New join-form fields
        assertEquals(FORMAT_ID, detail.getFormatId());
        assertFalse(detail.isCreator());
        assertFalse(detail.isAcceptedParticipant());
        assertFalse(detail.isHasPendingRequest());
    }

    @Test
    void shouldAllowCreatorToAccessOwnPublicListingRegardlessOfSportProfile() {
        // Creator requests own public listing — sport not required on profile
        long creatorId = 77L;
        fakeUserDao.addUser(new User(creatorId, "the_creator", "pass", "player"));
        GameListing listing = createPublicListing(LISTING_ID, creatorId, FORMAT_ID);
        fakeGameListingDao.addListing(listing);
        fakeSportFormatDao.addFormat(new SportFormat(FORMAT_ID, "3v3", false, 6, 60, SPORT_ID));
        fakeSportDao.addSport(new Sport(SPORT_ID, "Basketball"));
        fakeGameJoinerDao.setAcceptedCount(LISTING_ID, 1);
        fakeGameJoinerDao.setRoster(LISTING_ID, Map.of("A", List.of(new RosterEntryDto("the_creator", null))));

        // creatorId has no sports on profile — should still succeed because they are the creator
        ListingDetailDto detail = service.getListingDetail(creatorId, LISTING_ID);

        assertNotNull(detail);
        assertEquals(LISTING_ID, detail.getGameListingId());
        assertEquals("the_creator", detail.getCreatorUsername());
        // New join-form fields
        assertEquals(FORMAT_ID, detail.getFormatId());
        assertTrue(detail.isCreator());
        assertFalse(detail.isAcceptedParticipant());
        assertFalse(detail.isHasPendingRequest());
    }

    @Test
    void shouldAllowCreatorToAccessOwnPrivateListingWithoutInvitation() {
        // Creator requests own private listing — no invitation record needed
        long creatorId = 77L;
        fakeUserDao.addUser(new User(creatorId, "the_creator", "pass", "player"));
        GameListing listing = createPrivateListing(LISTING_ID, creatorId, FORMAT_ID);
        fakeGameListingDao.addListing(listing);
        fakeSportFormatDao.addFormat(new SportFormat(FORMAT_ID, "3v3", false, 6, 60, SPORT_ID));
        fakeSportDao.addSport(new Sport(SPORT_ID, "Basketball"));
        fakeGameJoinerDao.setAcceptedCount(LISTING_ID, 1);
        fakeGameJoinerDao.setRoster(LISTING_ID, Map.of("A", List.of(new RosterEntryDto("the_creator", null))));

        // No invitation for creatorId — should still succeed
        ListingDetailDto detail = service.getListingDetail(creatorId, LISTING_ID);

        assertNotNull(detail);
        assertEquals(LISTING_ID, detail.getGameListingId());
        assertTrue(detail.isPrivate());
        assertEquals("the_creator", detail.getCreatorUsername());
    }

    @Test
    void shouldPopulateAcceptedParticipantAndPendingRequestFields() {
        // User is an accepted joiner and has a pending request — both flags true
        GameListing listing = createPublicListing(LISTING_ID, OTHER_USER_ID, FORMAT_ID);
        fakeGameListingDao.addListing(listing);
        fakeSportFormatDao.addFormat(new SportFormat(FORMAT_ID, "3v3", false, 6, 60, SPORT_ID));
        fakeSportDao.addSport(new Sport(SPORT_ID, "Basketball"));
        fakeGameJoinerDao.setAcceptedCount(LISTING_ID, 2);
        fakeGameJoinerDao.setRoster(LISTING_ID, Map.of(
                "A", List.of(new RosterEntryDto("test_user", null)),
                "B", List.of(new RosterEntryDto("creator_user", null))
        ));
        fakeGameJoinerDao.addAcceptedJoiner(LISTING_ID, USER_ID);
        fakeJoinRequestDao.addPendingRequest(LISTING_ID, USER_ID);

        ListingDetailDto detail = service.getListingDetail(USER_ID, LISTING_ID);

        assertNotNull(detail);
        assertEquals(FORMAT_ID, detail.getFormatId());
        assertFalse(detail.isCreator());
        assertTrue(detail.isAcceptedParticipant());
        assertTrue(detail.isHasPendingRequest());
    }

    @Test
    void shouldReturnBothPositionNamesInRoster() {
        // Positional format with both primary and alternate positions
        GameListing listing = createPublicListing(LISTING_ID, OTHER_USER_ID, FORMAT_ID);
        fakeGameListingDao.addListing(listing);
        fakeSportFormatDao.addFormat(new SportFormat(FORMAT_ID, "3v3", true, 6, 60, SPORT_ID));
        fakeSportDao.addSport(new Sport(SPORT_ID, "Basketball"));
        fakeGameJoinerDao.setAcceptedCount(LISTING_ID, 2);
        fakeGameJoinerDao.setRoster(LISTING_ID, Map.of(
                "A", List.of(new RosterEntryDto("player1", "Centre", "Point Guard")),
                "B", List.of(new RosterEntryDto("player2", "Shooting Guard", "Small Forward"))
        ));

        ListingDetailDto detail = service.getListingDetail(USER_ID, LISTING_ID);

        assertNotNull(detail);
        assertEquals("Centre", detail.getTeamA().get(0).getPositionName());
        assertEquals("Point Guard", detail.getTeamA().get(0).getAlternatePositionName());
        assertEquals("Shooting Guard", detail.getTeamB().get(0).getPositionName());
        assertEquals("Small Forward", detail.getTeamB().get(0).getAlternatePositionName());
    }

    @Test
    void shouldReturnOnlyPrimaryPositionWhenNoAlternate() {
        // Positional format with only primary position set
        GameListing listing = createPublicListing(LISTING_ID, OTHER_USER_ID, FORMAT_ID);
        fakeGameListingDao.addListing(listing);
        fakeSportFormatDao.addFormat(new SportFormat(FORMAT_ID, "3v3", true, 6, 60, SPORT_ID));
        fakeSportDao.addSport(new Sport(SPORT_ID, "Basketball"));
        fakeGameJoinerDao.setAcceptedCount(LISTING_ID, 1);
        fakeGameJoinerDao.setRoster(LISTING_ID, Map.of(
                "A", List.of(new RosterEntryDto("player1", "Centre", null))
        ));

        ListingDetailDto detail = service.getListingDetail(USER_ID, LISTING_ID);

        assertNotNull(detail);
        assertEquals("Centre", detail.getTeamA().get(0).getPositionName());
        assertNull(detail.getTeamA().get(0).getAlternatePositionName());
    }

    @Test
    void shouldReturnNullPositionsForNonPositionalFormat() {
        // Non-positional format — both position fields should be null
        GameListing listing = createPublicListing(LISTING_ID, OTHER_USER_ID, FORMAT_ID);
        fakeGameListingDao.addListing(listing);
        fakeSportFormatDao.addFormat(new SportFormat(FORMAT_ID, "3v3", false, 6, 60, SPORT_ID));
        fakeSportDao.addSport(new Sport(SPORT_ID, "Basketball"));
        fakeGameJoinerDao.setAcceptedCount(LISTING_ID, 1);
        fakeGameJoinerDao.setRoster(LISTING_ID, Map.of(
                "A", List.of(new RosterEntryDto("player1", null, null))
        ));

        ListingDetailDto detail = service.getListingDetail(USER_ID, LISTING_ID);

        assertNotNull(detail);
        assertFalse(detail.isHasPositions());
        assertNull(detail.getTeamA().get(0).getPositionName());
        assertNull(detail.getTeamA().get(0).getAlternatePositionName());
    }

    // ========================================================
    // Helper methods
    // ========================================================

    private BrowseListingDto createBrowseDto(long id, String sportName, String formatName,
                                             int spotsFilled, int totalSpots) {
        BrowseListingDto dto = new BrowseListingDto();
        dto.setGameListingId(id);
        dto.setSportName(sportName);
        dto.setFormatName(formatName);
        dto.setSkillLevel("Intermediate");
        dto.setDate("2026-08-15");
        dto.setSessionWindow("14:00\u201315:00");
        dto.setLocation("University Fields");
        dto.setSpotsFilled(spotsFilled);
        dto.setTotalSpots(totalSpots);
        dto.setCreatorUsername("creator_user");
        return dto;
    }

    private GameListing createPublicListing(long listingId, long creatorId, long formatId) {
        return new GameListing(listingId,
                LocalDateTime.of(2026, 8, 15, 14, 0),
                LocalDateTime.of(2026, 8, 15, 15, 0),
                "OPEN", false, "University Fields", "Intermediate",
                creatorId, formatId);
    }

    private GameListing createPrivateListing(long listingId, long creatorId, long formatId) {
        return new GameListing(listingId,
                LocalDateTime.of(2026, 8, 15, 14, 0),
                LocalDateTime.of(2026, 8, 15, 15, 0),
                "OPEN", true, "University Fields", "Intermediate",
                creatorId, formatId);
    }

    // ========================================================
    // Fake implementations
    // ========================================================

    private static class FakeSportDao extends SportDao {
        private final Map<Long, Map<Long, String>> userSports = new HashMap<>();
        private final Map<Long, Sport> sports = new HashMap<>();

        FakeSportDao() { super(null); }

        void addUserSport(long userId, long sportId, String sportName) {
            userSports.computeIfAbsent(userId, k -> new HashMap<>()).put(sportId, sportName);
        }

        void addSport(Sport sport) {
            sports.put(sport.getSportId(), sport);
        }

        @Override
        public List<Sport> findSportsByUserId(long userId) {
            Map<Long, String> userSportMap = userSports.get(userId);
            if (userSportMap == null) return List.of();
            List<Sport> result = new ArrayList<>();
            for (Map.Entry<Long, String> e : userSportMap.entrySet()) {
                result.add(new Sport(e.getKey(), e.getValue()));
            }
            return result;
        }

        @Override
        public boolean userHasSport(long userId, long sportId) {
            Map<Long, String> userSportMap = userSports.get(userId);
            return userSportMap != null && userSportMap.containsKey(sportId);
        }

        @Override
        public Optional<Sport> findById(long sportId) {
            return Optional.ofNullable(sports.get(sportId));
        }
    }

    private static class FakeSportFormatDao extends SportFormatDao {
        private final Map<Long, SportFormat> formats = new HashMap<>();

        FakeSportFormatDao() { super(null); }

        void addFormat(SportFormat f) { formats.put(f.getFormatId(), f); }

        @Override
        public Optional<SportFormat> findById(long formatId) {
            return Optional.ofNullable(formats.get(formatId));
        }
    }

    private static class FakeGameListingDao extends GameListingDao {
        private final Map<Long, GameListing> listings = new HashMap<>();
        private List<BrowseListingDto> browseListings = List.of();
        private long browseCount = 0;

        FakeGameListingDao() { super(null); }

        void addListing(GameListing listing) {
            listings.put(listing.getGameListingId(), listing);
        }

        void setBrowseListings(List<BrowseListingDto> listings) {
            this.browseListings = listings;
        }

        void setBrowseCount(long count) {
            this.browseCount = count;
        }

        @Override
        public Optional<GameListing> findById(long gameListingId) {
            return Optional.ofNullable(listings.get(gameListingId));
        }

        @Override
        public List<BrowseListingDto> findBrowseListings(List<Long> userSportIds, BrowseFilter filter) {
            return browseListings;
        }

        @Override
        public long countBrowseListings(List<Long> userSportIds, BrowseFilter filter) {
            return browseCount;
        }
    }

    private static class FakeGameJoinerDao extends GameJoinerDao {
        private final Map<Long, Integer> acceptedCounts = new HashMap<>();
        private final Map<Long, Map<String, List<RosterEntryDto>>> rosters = new HashMap<>();
        private final Set<String> acceptedJoiners = new HashSet<>();

        FakeGameJoinerDao() { super(null); }

        void setAcceptedCount(long listingId, int count) {
            acceptedCounts.put(listingId, count);
        }

        void setRoster(long listingId, Map<String, List<RosterEntryDto>> roster) {
            rosters.put(listingId, roster);
        }

        void addAcceptedJoiner(long listingId, long userId) {
            acceptedJoiners.add(listingId + ":" + userId);
        }

        @Override
        public int countAcceptedByListingId(long gameListingId) {
            return acceptedCounts.getOrDefault(gameListingId, 0);
        }

        @Override
        public Map<String, List<RosterEntryDto>> findRosterByListingId(long gameListingId) {
            return rosters.getOrDefault(gameListingId, Map.of());
        }

        @Override
        public boolean isAcceptedJoiner(long gameListingId, long userId) {
            return acceptedJoiners.contains(gameListingId + ":" + userId);
        }
    }

    private static class FakeInvitationDao extends InvitationDao {
        private final Set<String> invitations = new HashSet<>();

        FakeInvitationDao() { super(null); }

        void addInvitation(long listingId, long userId) {
            invitations.add(listingId + ":" + userId);
        }

        @Override
        public boolean hasInvitation(long gameListingId, long userId) {
            return invitations.contains(gameListingId + ":" + userId);
        }
    }

    private static class FakeUserDao extends UserDao {
        private final Map<Long, User> users = new HashMap<>();

        FakeUserDao() { super(null); }

        void addUser(User user) {
            users.put(user.getUserId(), user);
        }

        @Override
        public Optional<User> findById(long userId) {
            return Optional.ofNullable(users.get(userId));
        }
    }

    private static class FakeJoinRequestDao extends JoinRequestDao {
        private final Set<String> pendingRequests = new HashSet<>();

        FakeJoinRequestDao() { super(null); }

        void addPendingRequest(long listingId, long userId) {
            pendingRequests.add(listingId + ":" + userId);
        }

        @Override
        public boolean hasPendingRequest(long gameListingId, long userId) {
            return pendingRequests.contains(gameListingId + ":" + userId);
        }
    }
}
