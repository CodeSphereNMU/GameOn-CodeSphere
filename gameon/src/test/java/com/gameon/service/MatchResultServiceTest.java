package com.gameon.service;

import com.gameon.exception.BusinessRuleException;
import com.gameon.exception.UnauthorizedAccessException;
import com.gameon.model.entity.*;
import com.gameon.model.enums.*;
import com.gameon.repository.GameJoinerRepository;
import com.gameon.repository.GameListingRepository;
import com.gameon.repository.MatchResultRepository;
import com.gameon.repository.UserSportProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchResultServiceTest {

    @Mock MatchResultRepository matchResultRepository;
    @Mock GameListingRepository gameListingRepository;
    @Mock GameJoinerRepository gameJoinerRepository;
    @Mock UserSportProfileRepository userSportProfileRepository;
    @Mock NotificationService notificationService;
    @InjectMocks MatchResultService service;

    private User creator;
    private User teamBUser;
    private Sport sport;
    private GameListing listing;
    private GameJoiner teamA;
    private GameJoiner teamB;
    private UserSportProfile teamAProfile;
    private UserSportProfile teamBProfile;

    @BeforeEach
    void setUp() {
        creator = user(1L, "Creator");
        teamBUser = user(2L, "PlayerB");
        sport = new Sport("Tennis");
        sport.setSportId(5L);
        SportFormat format = new SportFormat("Singles", 2, false, 60, sport);
        format.setFormatId(6L);
        listing = new GameListing(creator, format, SkillLevel.INTERMEDIATE,
                LocalDateTime.now().minusHours(2), "Court", PrivacySetting.PUBLIC, 60);
        listing.setGameListingId(10L);
        listing.setListingStatus(ListingStatus.CONFIRMED);

        teamA = new GameJoiner(creator, listing, Team.A);
        teamA.setStatus(JoinerStatus.LOCKED);
        teamB = new GameJoiner(teamBUser, listing, Team.B);
        teamB.setStatus(JoinerStatus.LOCKED);
        teamAProfile = profile(creator);
        teamBProfile = profile(teamBUser);
    }

    @Test
    void resultCannotBeSubmittedBeforeConfirmedGameEnds() {
        listing.setScheduledDate(LocalDateTime.now().minusMinutes(30));
        when(gameListingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> service.recordResult(10L, 1L, 2, 1))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("after the game has ended");
        verify(matchResultRepository, never()).save(any());
    }

    @Test
    void onlyCreatorCanSubmitOrCorrectResult() {
        when(gameListingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(listing));
        assertThatThrownBy(() -> service.recordResult(10L, 2L, 2, 1))
                .isInstanceOf(UnauthorizedAccessException.class);

        listing.setListingStatus(ListingStatus.COMPLETED);
        when(gameListingRepository.findById(10L)).thenReturn(Optional.of(listing));
        assertThatThrownBy(() -> service.updateResult(10L, 2L, 1, 2))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    void firstResultCompletesListingAndUpdatesOnlyLockedParticipantStats() {
        arrangeRecord();

        MatchResult result = service.recordResult(10L, 1L, 3, 1);

        assertThat(result.getWinners()).isEqualTo("TEAM_A");
        assertThat(listing.getListingStatus()).isEqualTo(ListingStatus.COMPLETED);
        assertThat(teamAProfile.getWins()).isEqualTo(1);
        assertThat(teamBProfile.getLosses()).isEqualTo(1);
        verify(gameJoinerRepository).findByIdGameListingIdAndStatus(10L, JoinerStatus.LOCKED);
        verify(gameListingRepository).save(listing);
    }

    @Test
    void drawCompletesListingWithoutAddingWinOrLoss() {
        arrangeRecord();

        MatchResult result = service.recordResult(10L, 1L, 2, 2);

        assertThat(result.getWinners()).isEqualTo("DRAW");
        assertThat(listing.getListingStatus()).isEqualTo(ListingStatus.COMPLETED);
        assertThat(teamAProfile.getWins()).isZero();
        assertThat(teamAProfile.getLosses()).isZero();
        assertThat(teamBProfile.getWins()).isZero();
        assertThat(teamBProfile.getLosses()).isZero();
    }

    @Test
    void correctionReversesOldWinnerBeforeApplyingNewWinner() {
        listing.setListingStatus(ListingStatus.COMPLETED);
        teamAProfile.setWins(1);
        teamBProfile.setLosses(1);
        MatchResult existing = new MatchResult(listing, 3, 1);
        when(gameListingRepository.findById(10L)).thenReturn(Optional.of(listing));
        when(matchResultRepository.findByGameListingGameListingId(10L)).thenReturn(Optional.of(existing));
        when(matchResultRepository.save(any(MatchResult.class))).thenAnswer(call -> call.getArgument(0));
        arrangeParticipantsAndProfiles();

        MatchResult corrected = service.updateResult(10L, 1L, 1, 4);

        assertThat(corrected.getWinners()).isEqualTo("TEAM_B");
        assertThat(teamAProfile.getWins()).isZero();
        assertThat(teamAProfile.getLosses()).isEqualTo(1);
        assertThat(teamBProfile.getWins()).isEqualTo(1);
        assertThat(teamBProfile.getLosses()).isZero();
    }

    private void arrangeRecord() {
        when(gameListingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(listing));
        when(matchResultRepository.existsByGameListingGameListingId(10L)).thenReturn(false);
        when(matchResultRepository.save(any(MatchResult.class))).thenAnswer(call -> call.getArgument(0));
        arrangeParticipantsAndProfiles();
    }

    private void arrangeParticipantsAndProfiles() {
        when(gameJoinerRepository.findByIdGameListingIdAndStatus(10L, JoinerStatus.LOCKED))
                .thenReturn(List.of(teamA, teamB));
        when(gameJoinerRepository.findParticipants(10L)).thenReturn(List.of(teamA, teamB));
        when(userSportProfileRepository.findByIdUserIdAndIdSportId(1L, 5L))
                .thenReturn(Optional.of(teamAProfile));
        when(userSportProfileRepository.findByIdUserIdAndIdSportId(2L, 5L))
                .thenReturn(Optional.of(teamBProfile));
    }

    private UserSportProfile profile(User user) {
        UserSportProfile profile = new UserSportProfile(user, sport, SkillLevel.INTERMEDIATE);
        profile.setWins(0);
        profile.setLosses(0);
        return profile;
    }

    private User user(Long id, String name) {
        User user = new User(name, "password", UserRole.USER);
        user.setUserId(id);
        return user;
    }
}
