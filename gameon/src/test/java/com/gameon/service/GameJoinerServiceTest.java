package com.gameon.service;

import com.gameon.exception.BusinessRuleException;
import com.gameon.model.entity.*;
import com.gameon.model.enums.*;
import com.gameon.repository.GameJoinerRepository;
import com.gameon.repository.GameListingRepository;
import com.gameon.repository.UserRepository;
import com.gameon.repository.UserSportProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameJoinerService Tests")
class GameJoinerServiceTest {

    @Mock
    private GameJoinerRepository gameJoinerRepository;

    @Mock
    private GameListingRepository gameListingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSportProfileRepository userSportProfileRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SchedulingConflictService schedulingConflictService;

    @Mock
    private SportService sportService;

    @Mock
    private InvitationService invitationService;

    @InjectMocks
    private GameJoinerService gameJoinerService;

    private User creator;
    private User joinerUser;
    private Sport testSport;
    private SportFormat testFormat;
    private GameListing testListing;

    @BeforeEach
    void setUp() {
        creator = new User("Creator", "password", UserRole.USER);
        creator.setUserId(1L);

        joinerUser = new User("Joiner", "password", UserRole.USER);
        joinerUser.setUserId(2L);

        testSport = new Sport("Football", 22);
        testSport.setSportId(1L);

        testFormat = new SportFormat("5v5", 10, true, testSport);
        testFormat.setFormatId(1L);

        testListing = new GameListing(creator, testFormat, SkillLevel.INTERMEDIATE,
                LocalDateTime.now().plusDays(14), "Location", PrivacySetting.PUBLIC, 2);
        testListing.setGameListingId(10L);
    }

    @Nested
    @DisplayName("Pending Request Rules")
    class PendingRequestRules {

        @Test
        @DisplayName("One pending request - second pending request rejected")
        void duplicatePending_rejected() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(joinerUser));
            when(gameListingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(testListing));
            when(userSportProfileRepository.existsByIdUserIdAndIdSportId(2L, 1L)).thenReturn(true);

            GameJoiner existingPending = new GameJoiner(joinerUser, testListing, Team.A);
            existingPending.setStatus(JoinerStatus.PENDING);
            when(gameJoinerRepository.findByUserAndListing(2L, 10L))
                    .thenReturn(Optional.of(existingPending));

            assertThatThrownBy(() -> gameJoinerService.sendJoinRequest(
                    2L, 10L, Team.A, null, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("already have a pending join request");
        }

        @Test
        @DisplayName("Accepted participant cannot submit another request")
        void acceptedParticipant_rejected() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(joinerUser));
            when(gameListingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(testListing));
            when(userSportProfileRepository.existsByIdUserIdAndIdSportId(2L, 1L)).thenReturn(true);

            GameJoiner existingAccepted = new GameJoiner(joinerUser, testListing, Team.A);
            existingAccepted.setStatus(JoinerStatus.ACCEPTED);
            when(gameJoinerRepository.findByUserAndListing(2L, 10L))
                    .thenReturn(Optional.of(existingAccepted));

            assertThatThrownBy(() -> gameJoinerService.sendJoinRequest(
                    2L, 10L, Team.A, null, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("already participating");
        }

        @Test
        @DisplayName("Rejected request - new request allowed (re-request)")
        void rejectedRequest_newAllowed() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(joinerUser));
            when(gameListingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(testListing));
            when(userSportProfileRepository.existsByIdUserIdAndIdSportId(2L, 1L)).thenReturn(true);

            GameJoiner existingRejected = new GameJoiner(joinerUser, testListing, Team.A);
            existingRejected.setStatus(JoinerStatus.REJECTED);
            when(gameJoinerRepository.findByUserAndListing(2L, 10L))
                    .thenReturn(Optional.of(existingRejected));

            when(schedulingConflictService.getConflictMessage(eq(2L), any(), eq(2), isNull()))
                    .thenReturn(null);
            when(gameJoinerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            GameJoiner result = gameJoinerService.sendJoinRequest(2L, 10L, Team.A, null, null);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(JoinerStatus.PENDING);
            verify(gameJoinerRepository).delete(existingRejected);
        }
    }

    @Nested
    @DisplayName("Sport Profile on Join")
    class SportProfileOnJoin {

        @Test
        @DisplayName("Sport on profile - join allowed")
        void sportOnProfile_allowed() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(joinerUser));
            when(gameListingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(testListing));
            when(userSportProfileRepository.existsByIdUserIdAndIdSportId(2L, 1L)).thenReturn(true);
            when(gameJoinerRepository.findByUserAndListing(2L, 10L)).thenReturn(Optional.empty());
            when(schedulingConflictService.getConflictMessage(eq(2L), any(), eq(2), isNull()))
                    .thenReturn(null);
            when(gameJoinerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            GameJoiner result = gameJoinerService.sendJoinRequest(2L, 10L, Team.A, null, null);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Sport not on profile - join rejected")
        void sportNotOnProfile_rejected() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(joinerUser));
            when(gameListingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(testListing));
            when(userSportProfileRepository.existsByIdUserIdAndIdSportId(2L, 1L)).thenReturn(false);

            assertThatThrownBy(() -> gameJoinerService.sendJoinRequest(
                    2L, 10L, Team.A, null, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("not included in your sports profile");
        }
    }

    @Nested
    @DisplayName("Scheduling Conflict on Join")
    class SchedulingConflictOnJoin {

        @Test
        @DisplayName("Scheduling conflict on join - rejected")
        void conflictOnJoin_rejected() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(joinerUser));
            when(gameListingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(testListing));
            when(userSportProfileRepository.existsByIdUserIdAndIdSportId(2L, 1L)).thenReturn(true);
            when(gameJoinerRepository.findByUserAndListing(2L, 10L)).thenReturn(Optional.empty());
            when(schedulingConflictService.getConflictMessage(eq(2L), any(), eq(2), isNull()))
                    .thenReturn("Conflict with existing session");

            assertThatThrownBy(() -> gameJoinerService.sendJoinRequest(
                    2L, 10L, Team.A, null, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Conflict with existing session");
        }
    }

    @Nested
    @DisplayName("Accept Request with Scheduling Conflict")
    class AcceptRequestConflict {

        @Test
        @DisplayName("Accept triggers scheduling conflict recheck - rejected if conflict")
        void acceptWithConflict_rejected() {
            when(gameListingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(testListing));

            GameJoiner pendingJoiner = new GameJoiner(joinerUser, testListing, Team.A);
            pendingJoiner.setStatus(JoinerStatus.PENDING);
            when(gameJoinerRepository.findById(new GameJoinerId(2L, 10L)))
                    .thenReturn(Optional.of(pendingJoiner));

            when(schedulingConflictService.getConflictMessage(eq(2L), any(), eq(2), isNull()))
                    .thenReturn("Conflict detected");

            assertThatThrownBy(() -> gameJoinerService.acceptRequest(10L, 2L, 1L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("scheduling conflict");
        }

        @Test
        @DisplayName("Accept with no conflict - succeeds")
        void acceptNoConflict_succeeds() {
            when(gameListingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(testListing));

            GameJoiner pendingJoiner = new GameJoiner(joinerUser, testListing, Team.A);
            pendingJoiner.setStatus(JoinerStatus.PENDING);
            when(gameJoinerRepository.findById(new GameJoinerId(2L, 10L)))
                    .thenReturn(Optional.of(pendingJoiner));

            when(schedulingConflictService.getConflictMessage(eq(2L), any(), eq(2), isNull()))
                    .thenReturn(null);
            when(gameJoinerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            GameJoiner result = gameJoinerService.acceptRequest(10L, 2L, 1L);
            assertThat(result.getStatus()).isEqualTo(JoinerStatus.ACCEPTED);
        }
    }

    @Nested
    @DisplayName("Two-hour request lock-in")
    class RequestLockIn {

        @Test
        @DisplayName("Accept is rejected at lock-in")
        void acceptAtLockIn_rejected() {
            testListing.setScheduledDate(LocalDateTime.now().plusHours(2));
            when(gameListingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(testListing));

            assertThatThrownBy(() -> gameJoinerService.acceptRequest(10L, 2L, 1L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("close 2 hours");
        }

        @Test
        @DisplayName("Reject is rejected at lock-in")
        void rejectAtLockIn_rejected() {
            testListing.setScheduledDate(LocalDateTime.now().plusHours(2));
            when(gameListingRepository.findById(10L)).thenReturn(Optional.of(testListing));

            assertThatThrownBy(() -> gameJoinerService.rejectRequest(10L, 2L, 1L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("close 2 hours");
        }
    }

    @Nested
    @DisplayName("Invitation Does Not Auto-Accept")
    class InvitationNoAutoAccept {

        @Test
        @DisplayName("Invited user must still submit join request")
        void invitedUser_mustSubmitRequest() {
            // After an invitation notification, the user still goes through sendJoinRequest
            // This test verifies the normal flow works for an invited user
            when(userRepository.findById(2L)).thenReturn(Optional.of(joinerUser));
            when(gameListingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(testListing));
            when(userSportProfileRepository.existsByIdUserIdAndIdSportId(2L, 1L)).thenReturn(true);
            when(gameJoinerRepository.findByUserAndListing(2L, 10L)).thenReturn(Optional.empty());
            when(schedulingConflictService.getConflictMessage(eq(2L), any(), eq(2), isNull()))
                    .thenReturn(null);
            when(gameJoinerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            GameJoiner result = gameJoinerService.sendJoinRequest(2L, 10L, Team.B, null, null);

            // The result is PENDING, not ACCEPTED - invitation does not auto-accept
            assertThat(result.getStatus()).isEqualTo(JoinerStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("Creator Cannot Join Own Listing")
    class CreatorCannotJoin {

        @Test
        @DisplayName("Creator trying to join own listing - rejected")
        void creatorJoinsOwn_rejected() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
            when(gameListingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(testListing));

            assertThatThrownBy(() -> gameJoinerService.sendJoinRequest(
                    1L, 10L, Team.A, null, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("already participating in this listing as the creator");
        }
    }
}
