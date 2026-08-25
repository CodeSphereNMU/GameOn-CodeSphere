package com.gameon.service;

import com.gameon.exception.BusinessRuleException;
import com.gameon.exception.ResourceNotFoundException;
import com.gameon.exception.UnauthorizedAccessException;
import com.gameon.model.entity.GameJoiner;
import com.gameon.model.entity.GameListing;
import com.gameon.model.entity.MatchResult;
import com.gameon.model.entity.Sport;
import com.gameon.model.entity.User;
import com.gameon.model.entity.UserSportProfile;
import com.gameon.model.enums.JoinerStatus;
import com.gameon.model.enums.NotificationType;
import com.gameon.model.enums.SkillLevel;
import com.gameon.model.enums.Team;
import com.gameon.repository.GameJoinerRepository;
import com.gameon.repository.GameListingRepository;
import com.gameon.repository.MatchResultRepository;
import com.gameon.repository.SportRepository;
import com.gameon.repository.UserRepository;
import com.gameon.repository.UserSportProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service handling match result recording and stat updates.
 * Covers C100 (Record Match Result), C200 (Update), C400 (View History).
 *
 * Business Rules enforced:
 * - BR10: One match result per Game Listing
 * - BR11: Only listing creator can update match result
 */
@Service
public class MatchResultService {

    private static final Logger logger = LoggerFactory.getLogger(MatchResultService.class);

    private final MatchResultRepository matchResultRepository;
    private final GameListingRepository gameListingRepository;
    private final GameJoinerRepository gameJoinerRepository;
    private final UserSportProfileRepository userSportProfileRepository;
    private final UserRepository userRepository;
    private final SportRepository sportRepository;
    private final NotificationService notificationService;

    public MatchResultService(MatchResultRepository matchResultRepository,
                              GameListingRepository gameListingRepository,
                              GameJoinerRepository gameJoinerRepository,
                              UserSportProfileRepository userSportProfileRepository,
                              UserRepository userRepository,
                              SportRepository sportRepository,
                              NotificationService notificationService) {
        this.matchResultRepository = matchResultRepository;
        this.gameListingRepository = gameListingRepository;
        this.gameJoinerRepository = gameJoinerRepository;
        this.userSportProfileRepository = userSportProfileRepository;
        this.userRepository = userRepository;
        this.sportRepository = sportRepository;
        this.notificationService = notificationService;
    }

    /**
     * Records a match result (C100).
     * BR10: Only one result per listing.
     * Only the listing creator can record.
     */
    @Transactional
    public MatchResult recordResult(Long listingId, Long creatorId, int teamAScore, int teamBScore) {
        GameListing listing = gameListingRepository.findByIdWithDetails(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Game Listing", listingId));

        // Only creator can record result
        if (!listing.getCreator().getUserId().equals(creatorId)) {
            throw new UnauthorizedAccessException("record match result for", "game listing");
        }

        // BR10: One result per listing
        if (matchResultRepository.existsByGameListingGameListingId(listingId)) {
            throw new BusinessRuleException("A match result has already been recorded for this listing.", "BR10");
        }

        // Create match result
        MatchResult result = new MatchResult(listing, teamAScore, teamBScore);
        MatchResult saved = matchResultRepository.save(result);

        // Mark listing as completed
        listing.setIsCompleted(true);
        gameListingRepository.save(listing);

        // Update win/loss stats for participants
        updateParticipantStats(listing, saved.getWinners());

        // Notify all participants
        String notifText = "Match result posted: Team A " + teamAScore + " - " + teamBScore +
                " Team B (" + saved.getWinners().replace("_", " ") + " wins!)";
        List<Long> participantIds = gameJoinerRepository.findParticipants(listingId).stream()
                .map(gj -> gj.getUser().getUserId())
                .toList();
        notificationService.createBulkNotifications(participantIds, notifText, NotificationType.MATCH_RESULT_POSTED);

        logger.info("Match result recorded: Listing {} | Score: {} - {} | Winner: {}",
                listingId, teamAScore, teamBScore, saved.getWinners());
        return saved;
    }

    /**
     * Updates a match result (C200).
     * BR11: Only listing creator can update.
     */
    @Transactional
    public MatchResult updateResult(Long listingId, Long creatorId, int teamAScore, int teamBScore) {
        GameListing listing = gameListingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Game Listing", listingId));

        // BR11: Only creator can update
        if (!listing.getCreator().getUserId().equals(creatorId)) {
            throw new UnauthorizedAccessException("update match result for", "game listing");
        }

        MatchResult result = matchResultRepository.findByGameListingGameListingId(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("No match result found for this listing"));

        String oldWinners = result.getWinners();

        result.setTeamAScore(teamAScore);
        result.setTeamBScore(teamBScore);
        result.setWinners(MatchResult.calculateWinner(teamAScore, teamBScore));

        MatchResult saved = matchResultRepository.save(result);

        // If winner changed, recalculate stats
        if (!oldWinners.equals(saved.getWinners())) {
            reverseParticipantStats(listing, oldWinners);
            updateParticipantStats(listing, saved.getWinners());
        }

        logger.info("Match result updated: Listing {} | New Score: {} - {} | Winner: {}",
                listingId, teamAScore, teamBScore, saved.getWinners());
        return saved;
    }

    /**
     * Gets match result for a listing.
     */
    @Transactional(readOnly = true)
    public MatchResult getResultForListing(Long listingId) {
        return matchResultRepository.findByGameListingGameListingId(listingId)
                .orElse(null);
    }

    /**
     * Gets match history for a user (as creator or participant).
     */
    @Transactional(readOnly = true)
    public List<MatchResult> getMatchHistory(Long userId) {
        return matchResultRepository.findMatchHistoryForUser(userId);
    }

    /**
     * Updates win/loss stats for all participants based on team and result.
     * Auto-creates UserSportProfile if missing (using listing skill level as default).
     */
    private void updateParticipantStats(GameListing listing, String winners) {
        Long sportId = listing.getFormat().getSport().getSportId();
        SkillLevel listingSkillLevel = listing.getSkillLevel();
        List<GameJoiner> participants = gameJoinerRepository.findByIdGameListingIdAndStatus(
                listing.getGameListingId(), JoinerStatus.LOCKED);

        // Also include creator (implicit Team A)
        updateStatForUser(listing.getCreator().getUserId(), sportId, winners, Team.A, listingSkillLevel);

        for (GameJoiner joiner : participants) {
            updateStatForUser(joiner.getUser().getUserId(), sportId, winners, joiner.getTeam(), listingSkillLevel);
        }
    }

    private void updateStatForUser(Long userId, Long sportId, String winners, Team userTeam, SkillLevel defaultSkillLevel) {
        UserSportProfile profile = ensureSportProfileExists(userId, sportId, defaultSkillLevel);
        if (profile == null) {
            logger.warn("Could not ensure sport profile for user {} sport {} - skipping stat update", userId, sportId);
            return;
        }

        boolean userWon = (userTeam == Team.A && "TEAM_A".equals(winners)) ||
                          (userTeam == Team.B && "TEAM_B".equals(winners));
        boolean isDraw = "DRAW".equals(winners);

        if (!isDraw) {
            if (userWon) {
                profile.setWins(profile.getWins() + 1);
            } else {
                profile.setLosses(profile.getLosses() + 1);
            }
            profile.calculateWinPercentage();
            userSportProfileRepository.save(profile);
        }
    }

    /**
     * Ensures a UserSportProfile exists for the given user and sport.
     * If no profile exists, auto-creates one with:
     *   - wins = 0, losses = 0, winPercentage = 0
     *   - skillLevel = listing skill level (if provided) or BEGINNER default
     *
     * This resolves the issue where an invited player can become LOCKED and
     * participate in a match without having a UserSportProfile for that sport.
     */
    private UserSportProfile ensureSportProfileExists(Long userId, Long sportId, SkillLevel defaultSkillLevel) {
        return userSportProfileRepository.findByIdUserIdAndIdSportId(userId, sportId)
                .orElseGet(() -> {
                    logger.info("Auto-creating UserSportProfile for user {} and sport {}", userId, sportId);
                    User user = userRepository.findById(userId).orElse(null);
                    Sport sport = sportRepository.findById(sportId).orElse(null);

                    if (user == null || sport == null) {
                        logger.warn("Cannot auto-create sport profile: user or sport not found. userId={}, sportId={}", userId, sportId);
                        return null;
                    }

                    SkillLevel skillLevel = (defaultSkillLevel != null) ? defaultSkillLevel : SkillLevel.BEGINNER;
                    UserSportProfile newProfile = new UserSportProfile(user, sport, skillLevel);
                    return userSportProfileRepository.save(newProfile);
                });
    }

    /**
     * Reverses stats when a result is corrected.
     */
    private void reverseParticipantStats(GameListing listing, String oldWinners) {
        Long sportId = listing.getFormat().getSport().getSportId();
        List<GameJoiner> participants = gameJoinerRepository.findByIdGameListingIdAndStatus(
                listing.getGameListingId(), JoinerStatus.LOCKED);

        reverseStatForUser(listing.getCreator().getUserId(), sportId, oldWinners, Team.A);

        for (GameJoiner joiner : participants) {
            reverseStatForUser(joiner.getUser().getUserId(), sportId, oldWinners, joiner.getTeam());
        }
    }

    private void reverseStatForUser(Long userId, Long sportId, String oldWinners, Team userTeam) {
        userSportProfileRepository.findByIdUserIdAndIdSportId(userId, sportId).ifPresent(profile -> {
            boolean userWon = (userTeam == Team.A && "TEAM_A".equals(oldWinners)) ||
                              (userTeam == Team.B && "TEAM_B".equals(oldWinners));
            boolean isDraw = "DRAW".equals(oldWinners);

            if (!isDraw) {
                if (userWon) {
                    profile.setWins(Math.max(0, profile.getWins() - 1));
                } else {
                    profile.setLosses(Math.max(0, profile.getLosses() - 1));
                }
                profile.calculateWinPercentage();
                userSportProfileRepository.save(profile);
            }
        });
    }
}
