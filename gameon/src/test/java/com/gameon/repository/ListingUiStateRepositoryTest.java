package com.gameon.repository;

import com.gameon.model.entity.GameJoiner;
import com.gameon.model.entity.GameListing;
import com.gameon.model.entity.JoinRequest;
import com.gameon.model.entity.MatchResult;
import com.gameon.model.entity.Sport;
import com.gameon.model.entity.SportFormat;
import com.gameon.model.entity.User;
import com.gameon.model.enums.JoinerStatus;
import com.gameon.model.enums.ListingStatus;
import com.gameon.model.enums.PrivacySetting;
import com.gameon.model.enums.SkillLevel;
import com.gameon.model.enums.Team;
import com.gameon.model.enums.UserRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class ListingUiStateRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2030, 1, 15, 12, 0);

    @Autowired private UserRepository userRepository;
    @Autowired private SportRepository sportRepository;
    @Autowired private SportFormatRepository sportFormatRepository;
    @Autowired private GameListingRepository gameListingRepository;
    @Autowired private GameJoinerRepository gameJoinerRepository;
    @Autowired private JoinRequestRepository joinRequestRepository;
    @Autowired private MatchResultRepository matchResultRepository;
    @Autowired private EntityManager entityManager;

    private User creator;
    private User participant;
    private User viewer;
    private SportFormat format;

    @BeforeEach
    void setUp() {
        creator = userRepository.save(new User("Creator", "password", UserRole.USER));
        participant = userRepository.save(new User("Participant", "password", UserRole.USER));
        viewer = userRepository.save(new User("Viewer", "password", UserRole.USER));

        Sport sport = sportRepository.save(new Sport("Football"));
        format = sportFormatRepository.save(new SportFormat("5v5", 10, true, 60, sport));
    }

    @Test
    void cancelledListingsAreExcludedFromBrowse() {
        GameListing open = listing(ListingStatus.OPEN, NOW.plusDays(1));
        listing(ListingStatus.CANCELLED_BY_CREATOR, NOW.plusDays(1));
        listing(ListingStatus.CANCELLED_INSUFFICIENT_PLAYERS, NOW.plusDays(1));

        entityManager.flush();
        entityManager.clear();

        List<GameListing> results = gameListingRepository.findAvailablePublicListings(
                List.of(format.getFormatId()), NOW, viewer.getUserId(), PageRequest.of(0, 20)).getContent();

        assertThat(results).extracting(GameListing::getGameListingId)
                .containsExactly(open.getGameListingId());
    }

    @Test
    void createdListingsContainOnlyNotStartedOpenOrConfirmedGames() {
        GameListing futureOpen = listing(ListingStatus.OPEN, NOW.plusHours(1));
        GameListing futureConfirmed = listing(ListingStatus.CONFIRMED, NOW.plusHours(2));
        listing(ListingStatus.CONFIRMED, NOW);
        listing(ListingStatus.COMPLETED, NOW.minusHours(2));
        listing(ListingStatus.CANCELLED_BY_CREATOR, NOW.plusHours(3));
        listing(ListingStatus.CANCELLED_INSUFFICIENT_PLAYERS, NOW.plusHours(4));

        entityManager.flush();
        entityManager.clear();

        List<GameListing> results = gameListingRepository.findCreatedByUser(creator.getUserId(), NOW);

        assertThat(results).extracting(GameListing::getGameListingId)
                .containsExactly(futureConfirmed.getGameListingId(), futureOpen.getGameListingId());
    }

    @Test
    void joinedListingsContainOnlyNotStartedActiveGames() {
        GameListing futureOpen = listing(ListingStatus.OPEN, NOW.plusHours(1));
        GameListing futureConfirmed = listing(ListingStatus.CONFIRMED, NOW.plusHours(2));
        GameListing started = listing(ListingStatus.CONFIRMED, NOW);
        GameListing cancelled = listing(ListingStatus.CANCELLED_BY_CREATOR, NOW.plusHours(3));

        addParticipant(futureOpen, JoinerStatus.ACCEPTED);
        addParticipant(futureConfirmed, JoinerStatus.LOCKED);
        addParticipant(started, JoinerStatus.LOCKED);
        addParticipant(cancelled, JoinerStatus.ACCEPTED);

        entityManager.flush();
        entityManager.clear();

        List<GameJoiner> results = gameJoinerRepository.findJoinedListingsForUser(
                participant.getUserId(), NOW);

        assertThat(results).extracting(joiner -> joiner.getGameListing().getGameListingId())
                .containsExactly(futureOpen.getGameListingId(), futureConfirmed.getGameListingId());
    }

    @Test
    void startedConfirmedGameAppearsInHistoryWithoutResult() {
        GameListing started = listing(ListingStatus.CONFIRMED, NOW);
        addParticipant(started, JoinerStatus.LOCKED);

        entityManager.flush();
        entityManager.clear();

        List<GameListing> results = gameListingRepository.findMatchHistoryForUser(
                participant.getUserId(), NOW);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getGameListingId()).isEqualTo(started.getGameListingId());
        assertThat(results.get(0).getMatchResult()).isNull();
    }

    @Test
    void completedGameRemainsInHistoryWithItsExistingResult() {
        GameListing completed = listing(ListingStatus.COMPLETED, NOW.minusHours(2));
        addParticipant(completed, JoinerStatus.LOCKED);
        matchResultRepository.save(new MatchResult(completed, 3, 1));

        entityManager.flush();
        entityManager.clear();

        List<GameListing> results = gameListingRepository.findMatchHistoryForUser(
                participant.getUserId(), NOW);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getGameListingId()).isEqualTo(completed.getGameListingId());
        assertThat(results.get(0).getMatchResult()).isNotNull();
        assertThat(results.get(0).getMatchResult().getTeamAScore()).isEqualTo(3);
        assertThat(results.get(0).getMatchResult().getTeamBScore()).isEqualTo(1);
    }

    @Test
    void cancelledAndFutureGamesAreExcludedFromHistory() {
        GameListing startedConfirmed = listing(ListingStatus.CONFIRMED, NOW.minusMinutes(1));
        addParticipant(startedConfirmed, JoinerStatus.LOCKED);
        GameListing futureConfirmed = listing(ListingStatus.CONFIRMED, NOW.plusMinutes(1));
        addParticipant(futureConfirmed, JoinerStatus.LOCKED);
        GameListing creatorCancelled = listing(ListingStatus.CANCELLED_BY_CREATOR, NOW.minusHours(1));
        addParticipant(creatorCancelled, JoinerStatus.LOCKED);
        GameListing insufficient = listing(ListingStatus.CANCELLED_INSUFFICIENT_PLAYERS, NOW.minusHours(2));
        addParticipant(insufficient, JoinerStatus.LOCKED);

        entityManager.flush();
        entityManager.clear();

        List<GameListing> results = gameListingRepository.findMatchHistoryForUser(
                participant.getUserId(), NOW);

        assertThat(results).extracting(GameListing::getGameListingId)
                .containsExactly(startedConfirmed.getGameListingId());
    }

    @Test
    void pendingRequestsAtOrInsideLockInAreNotReturnedAsActive() {
        GameListing insideLockIn = listing(ListingStatus.OPEN, NOW.plusHours(2));
        GameListing outsideLockIn = listing(ListingStatus.OPEN, NOW.plusHours(2).plusMinutes(1));
        JoinRequest expiredByTime = joinRequestRepository.save(
                new JoinRequest(participant, insideLockIn, Team.A));
        JoinRequest active = joinRequestRepository.save(
                new JoinRequest(participant, outsideLockIn, Team.B));

        entityManager.flush();
        entityManager.clear();

        LocalDateTime cutoff = NOW.plusHours(2);
        assertThat(joinRequestRepository.findPendingForCreator(insideLockIn.getGameListingId(), cutoff))
                .isEmpty();
        assertThat(joinRequestRepository.findPendingForCreator(outsideLockIn.getGameListingId(), cutoff))
                .extracting(JoinRequest::getJoinRequestId)
                .containsExactly(active.getJoinRequestId());
        assertThat(joinRequestRepository.findActiveForUser(participant.getUserId(), cutoff))
                .extracting(JoinRequest::getJoinRequestId)
                .containsExactly(active.getJoinRequestId())
                .doesNotContain(expiredByTime.getJoinRequestId());
    }

    private GameListing listing(ListingStatus status, LocalDateTime scheduledDate) {
        GameListing listing = new GameListing(creator, format, SkillLevel.INTERMEDIATE,
                scheduledDate, "Test Field", PrivacySetting.PUBLIC, 60);
        listing.setListingStatus(status);
        return gameListingRepository.save(listing);
    }

    private void addParticipant(GameListing listing, JoinerStatus status) {
        GameJoiner joiner = new GameJoiner(participant, listing, Team.B);
        joiner.setStatus(status);
        gameJoinerRepository.save(joiner);
    }
}
