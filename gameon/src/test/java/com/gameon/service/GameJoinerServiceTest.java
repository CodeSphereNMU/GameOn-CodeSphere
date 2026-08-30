package com.gameon.service;

import com.gameon.exception.BusinessRuleException;
import com.gameon.model.entity.*;
import com.gameon.model.enums.*;
import com.gameon.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
class GameJoinerServiceTest {

    @Mock GameJoinerRepository gameJoinerRepository;
    @Mock JoinRequestRepository joinRequestRepository;
    @Mock GameListingRepository gameListingRepository;
    @Mock UserRepository userRepository;
    @Mock UserSportProfileRepository userSportProfileRepository;
    @Mock NotificationService notificationService;
    @Mock SchedulingConflictService schedulingConflictService;
    @Mock SportService sportService;
    @Mock InvitationRepository invitationRepository;
    @Mock ListingLifecycleService listingLifecycleService;
    @InjectMocks GameJoinerService service;

    private User creator;
    private User requester;
    private GameListing listing;

    @BeforeEach
    void setUp() {
        creator = new User("Creator", "password", UserRole.USER);
        creator.setUserId(1L);
        requester = new User("Requester", "password", UserRole.USER);
        requester.setUserId(2L);
        Sport sport = new Sport("Football");
        sport.setSportId(1L);
        SportFormat format = new SportFormat("5v5", 10, false, sport);
        format.setFormatId(1L);
        format.setDurationMinutes(120);
        listing = new GameListing(creator, format, SkillLevel.INTERMEDIATE,
                LocalDateTime.now().plusDays(2), "Field", PrivacySetting.PUBLIC, 120);
        listing.setGameListingId(10L);
    }

    // ========== Existing Join Request Tests ==========

    @Nested
    @DisplayName("Join Request Submission")
    class JoinRequestTests {

        @Test
        void duplicatePendingRequestIsRejected() {
            arrangeRequestStart();
            when(joinRequestRepository.existsByGameListingGameListingIdAndUserUserIdAndStatus(
                    10L, 2L, JoinRequestStatus.PENDING)).thenReturn(true);

            assertThatThrownBy(() -> service.sendJoinRequest(2L, 10L, Team.A, null, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("already have a pending join request");
        }

        @Test
        void acceptedParticipantCannotRequestAgain() {
            arrangeRequestStart();
            when(gameJoinerRepository.existsAcceptedOrLocked(2L, 10L)).thenReturn(true);

            assertThatThrownBy(() -> service.sendJoinRequest(2L, 10L, Team.A, null, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("already participating");
        }

        @Test
        void rejectedHistoryDoesNotBlockANewRequest() {
            arrangeSuccessfulRequest();

            JoinRequest result = service.sendJoinRequest(2L, 10L, Team.A, null, null);

            assertThat(result.getStatus()).isEqualTo(JoinRequestStatus.PENDING);
            verify(joinRequestRepository).save(any(JoinRequest.class));
            verify(gameJoinerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Request rejected at T-1h (new cutoff)")
        void requestIsRejectedAtOneHourCutoff() {
            listing.setScheduledDate(LocalDateTime.now().plusHours(1));
            arrangeRequestStart();
            // isRequestWindowOpen depends on ListingLifecycleService.FINALISATION_HOURS = 1

            assertThatThrownBy(() -> service.sendJoinRequest(2L, 10L, Team.A, null, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("close 1 hour");
        }

        @Test
        @DisplayName("New join request can be submitted during T-2h to T-1h")
        void requestAcceptedDuringLastCallPeriod() {
            // Listing 90 minutes from now — past T-2h but before T-1h
            listing.setScheduledDate(LocalDateTime.now().plusMinutes(90));
            arrangeSuccessfulRequest();

            JoinRequest result = service.sendJoinRequest(2L, 10L, Team.A, null, null);

            assertThat(result.getStatus()).isEqualTo(JoinRequestStatus.PENDING);
        }

        @Test
        void creatorCannotRequestOwnListing() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
            when(gameListingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(listing));

            assertThatThrownBy(() -> service.sendJoinRequest(1L, 10L, Team.A, null, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("creator");
        }
    }

    // ========== Attendance Confirmation Tests ==========

    @Nested
    @DisplayName("Attendance Confirmation")
    class AttendanceConfirmationTests {

        @Test
        @DisplayName("Accepted player can confirm attendance when window is open")
        void confirmedPlayerCanConfirm() {
            when(gameListingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(listing));
            when(listingLifecycleService.isConfirmationWindowOpen(listing)).thenReturn(true);

            GameJoiner joiner = makeJoiner(2L, listing, JoinerStatus.ACCEPTED);
            when(gameJoinerRepository.findByUserAndListing(2L, 10L)).thenReturn(Optional.of(joiner));
            when(gameJoinerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            GameJoiner result = service.confirmAttendance(2L, 10L);

            assertThat(result.getStatus()).isEqualTo(JoinerStatus.CONFIRMED_ATTENDANCE);
            assertThat(result.getAttendanceConfirmedAt()).isNotNull();
        }

        @Test
        @DisplayName("Confirmation fails when window is not yet open")
        void confirmationFailsBeforeWindow() {
            when(gameListingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(listing));
            when(listingLifecycleService.isConfirmationWindowOpen(listing)).thenReturn(false);

            assertThatThrownBy(() -> service.confirmAttendance(2L, 10L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("not yet available");
        }

        @Test
        @DisplayName("Already confirmed player cannot confirm again")
        void alreadyConfirmedCannotConfirmAgain() {
            when(gameListingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(listing));
            when(listingLifecycleService.isConfirmationWindowOpen(listing)).thenReturn(true);

            GameJoiner joiner = makeJoiner(2L, listing, JoinerStatus.CONFIRMED_ATTENDANCE);
            when(gameJoinerRepository.findByUserAndListing(2L, 10L)).thenReturn(Optional.of(joiner));

            assertThatThrownBy(() -> service.confirmAttendance(2L, 10L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("already confirmed");
        }

        @Test
        @DisplayName("Confirmed player can withdraw normally before T-2h")
        void confirmedPlayerCanWithdrawNormally() {
            when(gameListingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(listing));
            when(listingLifecycleService.isPastFinalisation(listing)).thenReturn(false);
            when(listingLifecycleService.isInLastCallPeriod(listing)).thenReturn(false);

            GameJoiner joiner = makeJoiner(2L, listing, JoinerStatus.CONFIRMED_ATTENDANCE);
            when(gameJoinerRepository.findByUserAndListing(2L, 10L)).thenReturn(Optional.of(joiner));

            service.leaveListing(2L, 10L);

            assertThat(joiner.getStatus()).isEqualTo(JoinerStatus.LEFT);
            assertThat(joiner.isLateWithdrawal()).isFalse();
        }
    }

    // ========== Late Withdrawal Tests ==========

    @Nested
    @DisplayName("Late Withdrawal (T-2h to T-1h)")
    class LateWithdrawalTests {

        @Test
        @DisplayName("Withdrawal during T-2h to T-1h is recorded as late withdrawal")
        void withdrawalDuringLastCallIsLate() {
            when(gameListingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(listing));
            when(listingLifecycleService.isPastFinalisation(listing)).thenReturn(false);
            when(listingLifecycleService.isInLastCallPeriod(listing)).thenReturn(true);

            GameJoiner joiner = makeJoiner(2L, listing, JoinerStatus.CONFIRMED_ATTENDANCE);
            when(gameJoinerRepository.findByUserAndListing(2L, 10L)).thenReturn(Optional.of(joiner));

            service.leaveListing(2L, 10L);

            assertThat(joiner.getStatus()).isEqualTo(JoinerStatus.LEFT);
            assertThat(joiner.isLateWithdrawal()).isTrue();
            verify(gameJoinerRepository).save(joiner);
        }

        @Test
        @DisplayName("Late withdrawal releases capacity")
        void lateWithdrawalReleasesCapacity() {
            when(gameListingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(listing));
            when(listingLifecycleService.isPastFinalisation(listing)).thenReturn(false);
            when(listingLifecycleService.isInLastCallPeriod(listing)).thenReturn(true);

            GameJoiner joiner = makeJoiner(2L, listing, JoinerStatus.CONFIRMED_ATTENDANCE);
            when(gameJoinerRepository.findByUserAndListing(2L, 10L)).thenReturn(Optional.of(joiner));

            boolean result = service.leaveListing(2L, 10L);

            assertThat(result).isFalse(); // participant left, not request withdrawn
            assertThat(joiner.getStatus()).isEqualTo(JoinerStatus.LEFT);
        }

        @Test
        @DisplayName("Withdrawal unavailable from T-1h onward")
        void withdrawalUnavailableAfterFinalisation() {
            when(gameListingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(listing));
            when(listingLifecycleService.isPastFinalisation(listing)).thenReturn(true);

            assertThatThrownBy(() -> service.leaveListing(2L, 10L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("no longer available");
        }
    }

    // ========== Last-Call Replacement Tests ==========

    @Nested
    @DisplayName("Last-Call Replacement")
    class LastCallTests {

        @Test
        @DisplayName("Creator can select multiple requesters for last-call notification")
        void creatorSelectsMultipleRequesters() {
            when(gameListingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(listing));
            when(listingLifecycleService.isInLastCallPeriod(listing)).thenReturn(true);
            when(gameJoinerRepository.countByIdGameListingIdAndStatusIn(eq(10L), anyList())).thenReturn(8L); // 8 of 10

            JoinRequest req1 = new JoinRequest(requester, listing, Team.A);
            req1.setJoinRequestId(100L);
            User user3 = new User("User3", "pass", UserRole.USER);
            user3.setUserId(3L);
            JoinRequest req2 = new JoinRequest(user3, listing, Team.B);
            req2.setJoinRequestId(101L);

            when(joinRequestRepository.findFirstByGameListingGameListingIdAndUserUserIdAndStatusOrderByCreatedAtDesc(
                    10L, 2L, JoinRequestStatus.PENDING)).thenReturn(Optional.of(req1));
            when(joinRequestRepository.findFirstByGameListingGameListingIdAndUserUserIdAndStatusOrderByCreatedAtDesc(
                    10L, 3L, JoinRequestStatus.PENDING)).thenReturn(Optional.of(req2));

            service.approveLastCallRequesters(10L, 1L, List.of(2L, 3L));

            assertThat(req1.isLastCallApproved()).isTrue();
            assertThat(req2.isLastCallApproved()).isTrue();
            verify(notificationService, times(2)).createNotification(anyLong(), contains("place has opened"),
                    eq(NotificationType.LAST_CALL_OFFER), eq(creator), eq(listing), any(), isNull());
        }

        @Test
        @DisplayName("First eligible notified user can claim available capacity")
        void firstUserCanClaim() {
            when(gameListingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(listing));
            when(listingLifecycleService.isInLastCallPeriod(listing)).thenReturn(true);

            JoinRequest request = new JoinRequest(requester, listing, Team.A);
            request.setJoinRequestId(100L);
            request.setLastCallApproved(true);
            when(joinRequestRepository.findFirstByGameListingGameListingIdAndUserUserIdAndStatusOrderByCreatedAtDesc(
                    10L, 2L, JoinRequestStatus.PENDING)).thenReturn(Optional.of(request));

            when(gameJoinerRepository.countByIdGameListingIdAndStatusIn(eq(10L), anyList())).thenReturn(9L); // 9 of 10
            when(gameJoinerRepository.countByIdGameListingIdAndTeamAndStatus(eq(10L), eq(Team.A), eq(JoinerStatus.ACCEPTED))).thenReturn(0L);
            when(gameJoinerRepository.countByIdGameListingIdAndTeamAndStatus(eq(10L), eq(Team.A), eq(JoinerStatus.CONFIRMED_ATTENDANCE))).thenReturn(4L);
            when(gameJoinerRepository.countByIdGameListingIdAndTeamAndStatus(eq(10L), eq(Team.A), eq(JoinerStatus.LOCKED))).thenReturn(0L);
            when(schedulingConflictService.getConflictMessageMinutes(eq(2L), any(), eq(120), isNull())).thenReturn(null);
            when(gameJoinerRepository.findByUserAndListing(2L, 10L)).thenReturn(Optional.empty());
            when(gameJoinerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            GameJoiner result = service.claimLastCallPlace(2L, 10L);

            assertThat(result.getStatus()).isEqualTo(JoinerStatus.CONFIRMED_ATTENDANCE);
            assertThat(result.getAttendanceConfirmedAt()).isNotNull();
            assertThat(request.getStatus()).isEqualTo(JoinRequestStatus.ACCEPTED);
        }

        @Test
        @DisplayName("Successful last-call claim counts as confirmed attendance")
        void lastCallClaimIsConfirmed() {
            when(gameListingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(listing));
            when(listingLifecycleService.isInLastCallPeriod(listing)).thenReturn(true);

            JoinRequest request = new JoinRequest(requester, listing, Team.B);
            request.setJoinRequestId(100L);
            request.setLastCallApproved(true);
            when(joinRequestRepository.findFirstByGameListingGameListingIdAndUserUserIdAndStatusOrderByCreatedAtDesc(
                    10L, 2L, JoinRequestStatus.PENDING)).thenReturn(Optional.of(request));

            when(gameJoinerRepository.countByIdGameListingIdAndStatusIn(eq(10L), anyList())).thenReturn(9L);
            when(gameJoinerRepository.countByIdGameListingIdAndTeamAndStatus(eq(10L), eq(Team.B), eq(JoinerStatus.ACCEPTED))).thenReturn(0L);
            when(gameJoinerRepository.countByIdGameListingIdAndTeamAndStatus(eq(10L), eq(Team.B), eq(JoinerStatus.CONFIRMED_ATTENDANCE))).thenReturn(4L);
            when(gameJoinerRepository.countByIdGameListingIdAndTeamAndStatus(eq(10L), eq(Team.B), eq(JoinerStatus.LOCKED))).thenReturn(0L);
            when(schedulingConflictService.getConflictMessageMinutes(eq(2L), any(), eq(120), isNull())).thenReturn(null);
            when(gameJoinerRepository.findByUserAndListing(2L, 10L)).thenReturn(Optional.empty());
            when(gameJoinerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            GameJoiner result = service.claimLastCallPlace(2L, 10L);

            // No further confirmation needed
            assertThat(result.getStatus()).isEqualTo(JoinerStatus.CONFIRMED_ATTENDANCE);
            assertThat(result.getAttendanceConfirmedAt()).isNotNull();
        }

        @Test
        @DisplayName("Capacity cannot be exceeded when multiple users attempt to claim final place")
        void capacityCannotBeExceeded() {
            when(gameListingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(listing));
            when(listingLifecycleService.isInLastCallPeriod(listing)).thenReturn(true);

            JoinRequest request = new JoinRequest(requester, listing, Team.A);
            request.setJoinRequestId(100L);
            request.setLastCallApproved(true);
            when(joinRequestRepository.findFirstByGameListingGameListingIdAndUserUserIdAndStatusOrderByCreatedAtDesc(
                    10L, 2L, JoinRequestStatus.PENDING)).thenReturn(Optional.of(request));

            // Full capacity
            when(gameJoinerRepository.countByIdGameListingIdAndStatusIn(eq(10L), anyList())).thenReturn(10L);

            assertThatThrownBy(() -> service.claimLastCallPlace(2L, 10L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("already been filled");
        }

        @Test
        @DisplayName("Scheduling conflicts are rechecked when a last-call place is claimed")
        void schedulingConflictRecheckedOnClaim() {
            when(gameListingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(listing));
            when(listingLifecycleService.isInLastCallPeriod(listing)).thenReturn(true);

            JoinRequest request = new JoinRequest(requester, listing, Team.A);
            request.setJoinRequestId(100L);
            request.setLastCallApproved(true);
            when(joinRequestRepository.findFirstByGameListingGameListingIdAndUserUserIdAndStatusOrderByCreatedAtDesc(
                    10L, 2L, JoinRequestStatus.PENDING)).thenReturn(Optional.of(request));

            when(gameJoinerRepository.countByIdGameListingIdAndStatusIn(eq(10L), anyList())).thenReturn(9L);
            when(gameJoinerRepository.countByIdGameListingIdAndTeamAndStatus(eq(10L), eq(Team.A), eq(JoinerStatus.ACCEPTED))).thenReturn(0L);
            when(gameJoinerRepository.countByIdGameListingIdAndTeamAndStatus(eq(10L), eq(Team.A), eq(JoinerStatus.CONFIRMED_ATTENDANCE))).thenReturn(4L);
            when(gameJoinerRepository.countByIdGameListingIdAndTeamAndStatus(eq(10L), eq(Team.A), eq(JoinerStatus.LOCKED))).thenReturn(0L);
            when(schedulingConflictService.getConflictMessageMinutes(eq(2L), any(), eq(120), isNull()))
                    .thenReturn("Conflict with another session");

            assertThatThrownBy(() -> service.claimLastCallPlace(2L, 10L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Conflict");
        }

        @Test
        @DisplayName("Last-call claims unavailable from T-1h onward")
        void lastCallClaimUnavailableAfterFinalisation() {
            when(gameListingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(listing));
            when(listingLifecycleService.isInLastCallPeriod(listing)).thenReturn(false);

            assertThatThrownBy(() -> service.claimLastCallPlace(2L, 10L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("last-call period has ended");
        }

        @Test
        @DisplayName("Non-approved requester cannot claim")
        void nonApprovedCannotClaim() {
            when(gameListingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(listing));
            when(listingLifecycleService.isInLastCallPeriod(listing)).thenReturn(true);

            JoinRequest request = new JoinRequest(requester, listing, Team.A);
            request.setJoinRequestId(100L);
            request.setLastCallApproved(false);
            when(joinRequestRepository.findFirstByGameListingGameListingIdAndUserUserIdAndStatusOrderByCreatedAtDesc(
                    10L, 2L, JoinRequestStatus.PENDING)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> service.claimLastCallPlace(2L, 10L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("not been approved");
        }

        @Test
        @DisplayName("Selected requester becomes is_last_call_approved = true after approval")
        void selectedRequesterBecomesLastCallApproved() {
            when(gameListingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(listing));
            when(listingLifecycleService.isInLastCallPeriod(listing)).thenReturn(true);
            when(gameJoinerRepository.countByIdGameListingIdAndStatusIn(eq(10L), anyList())).thenReturn(8L);

            JoinRequest request = new JoinRequest(requester, listing, Team.A);
            request.setJoinRequestId(100L);
            assertThat(request.isLastCallApproved()).isFalse(); // starts false

            when(joinRequestRepository.findFirstByGameListingGameListingIdAndUserUserIdAndStatusOrderByCreatedAtDesc(
                    10L, 2L, JoinRequestStatus.PENDING)).thenReturn(Optional.of(request));

            service.approveLastCallRequesters(10L, 1L, List.of(2L));

            assertThat(request.isLastCallApproved()).isTrue(); // now true
            verify(joinRequestRepository).save(request);
        }

        @Test
        @DisplayName("Last-call notification is created for the selected requester")
        void lastCallNotificationCreatedForRequester() {
            when(gameListingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(listing));
            when(listingLifecycleService.isInLastCallPeriod(listing)).thenReturn(true);
            when(gameJoinerRepository.countByIdGameListingIdAndStatusIn(eq(10L), anyList())).thenReturn(8L);

            JoinRequest request = new JoinRequest(requester, listing, Team.A);
            request.setJoinRequestId(100L);
            when(joinRequestRepository.findFirstByGameListingGameListingIdAndUserUserIdAndStatusOrderByCreatedAtDesc(
                    10L, 2L, JoinRequestStatus.PENDING)).thenReturn(Optional.of(request));

            service.approveLastCallRequesters(10L, 1L, List.of(2L));

            verify(notificationService).createNotification(
                    eq(2L),                                    // recipient = requester
                    contains("place has opened"),              // notification text
                    eq(NotificationType.LAST_CALL_OFFER),      // correct type
                    eq(creator),                               // actor = creator
                    eq(listing),                               // listing reference
                    eq(request),                               // join request reference
                    isNull());                                 // no match result
        }
    }

    // ========== Last-Call Window Accept/Reject Restrictions ==========

    @Nested
    @DisplayName("Accept/Reject restrictions during last-call window")
    class LastCallRequestManagementRestrictionTests {

        @BeforeEach
        void arrangeLastCallWindow() {
            // Listing 90 minutes out: past T-2h, before T-1h, so request window is open
            // and the last-call period is active.
            listing.setScheduledDate(LocalDateTime.now().plusMinutes(90));
            when(gameListingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(listing));
        }

        @Test
        @DisplayName("Creator cannot directly accept during last-call window")
        void acceptBlockedDuringLastCall() {
            when(listingLifecycleService.isInLastCallPeriod(listing)).thenReturn(true);

            assertThatThrownBy(() -> service.acceptRequest(10L, 2L, 1L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("last-call offer");

            verify(gameJoinerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Pending requester without a last-call offer can still be rejected")
        void rejectAllowedForNonOfferedRequester() {
            JoinRequest request = new JoinRequest(requester, listing, Team.A);
            request.setJoinRequestId(100L);
            request.setLastCallApproved(false);
            when(joinRequestRepository.findFirstByGameListingGameListingIdAndUserUserIdAndStatusOrderByCreatedAtDesc(
                    10L, 2L, JoinRequestStatus.PENDING)).thenReturn(Optional.of(request));
            when(joinRequestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            JoinRequest result = service.rejectRequest(10L, 2L, 1L);

            assertThat(result.getStatus()).isEqualTo(JoinRequestStatus.REJECTED);
            verify(notificationService).createNotification(eq(2L), anyString(),
                    eq(NotificationType.JOIN_REJECTED), any(), eq(listing), eq(request), isNull());
        }

        @Test
        @DisplayName("Requester with an outstanding last-call offer cannot be rejected")
        void rejectBlockedForOfferedRequester() {
            JoinRequest request = new JoinRequest(requester, listing, Team.A);
            request.setJoinRequestId(100L);
            request.setLastCallApproved(true);
            when(joinRequestRepository.findFirstByGameListingGameListingIdAndUserUserIdAndStatusOrderByCreatedAtDesc(
                    10L, 2L, JoinRequestStatus.PENDING)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> service.rejectRequest(10L, 2L, 1L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("outstanding last-call offer");

            assertThat(request.getStatus()).isEqualTo(JoinRequestStatus.PENDING);
            verify(joinRequestRepository, never()).save(any());
        }
    }

    // ========== Browse Listings / Request Window Tests ==========

    @Nested
    @DisplayName("Request Window (Browse Listings Timing)")
    class RequestWindowTests {

        @Test
        @DisplayName("Request window open before T-1h")
        void requestWindowOpenBeforeT1h() {
            listing.setScheduledDate(LocalDateTime.now().plusHours(2));
            assertThat(service.isRequestWindowOpen(listing)).isTrue();
        }

        @Test
        @DisplayName("Request window closed at T-1h")
        void requestWindowClosedAtT1h() {
            listing.setScheduledDate(LocalDateTime.now().plusMinutes(59));
            assertThat(service.isRequestWindowOpen(listing)).isFalse();
        }

        @Test
        @DisplayName("Request window closed for non-OPEN listing")
        void requestWindowClosedForNonOpen() {
            listing.setListingStatus(ListingStatus.CONFIRMED);
            listing.setScheduledDate(LocalDateTime.now().plusDays(2));
            assertThat(service.isRequestWindowOpen(listing)).isFalse();
        }

        @Test
        @DisplayName("Backend rejects join request at T-1h even if UI state is stale")
        void backendRejectsAtT1h() {
            listing.setScheduledDate(LocalDateTime.now().plusMinutes(50));
            arrangeRequestStart();

            assertThatThrownBy(() -> service.sendJoinRequest(2L, 10L, Team.A, null, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("close 1 hour");
        }
    }

    // ========== Helper Methods ==========

    private void arrangeRequestStart() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(requester));
        when(gameListingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(listing));
    }

    private void arrangeSuccessfulRequest() {
        arrangeRequestStart();
        when(userSportProfileRepository.existsByIdUserIdAndIdSportId(2L, 1L)).thenReturn(true);
        when(schedulingConflictService.getConflictMessageMinutes(eq(2L), any(), eq(120), isNull()))
                .thenReturn(null);
        when(joinRequestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private GameJoiner makeJoiner(Long userId, GameListing listing, JoinerStatus status) {
        User user = new User("User" + userId, "password", UserRole.USER);
        user.setUserId(userId);
        GameJoiner joiner = new GameJoiner(user, listing, userId % 2 == 0 ? Team.B : Team.A);
        joiner.setStatus(status);
        if (status == JoinerStatus.CONFIRMED_ATTENDANCE) {
            joiner.setAttendanceConfirmedAt(LocalDateTime.now().minusHours(1));
        }
        return joiner;
    }
}
