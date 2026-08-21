package com.gameon.service;

import com.gameon.exception.BusinessRuleException;
import com.gameon.model.entity.*;
import com.gameon.model.enums.*;
import com.gameon.repository.*;
import org.junit.jupiter.api.BeforeEach;
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
    void requestCanRemainPendingWhenRosterIsCurrentlyFull() {
        arrangeSuccessfulRequest();

        JoinRequest result = service.sendJoinRequest(2L, 10L, Team.B, null, null);

        assertThat(result.getStatus()).isEqualTo(JoinRequestStatus.PENDING);
        verify(gameJoinerRepository, never()).countByIdGameListingIdAndStatusIn(anyLong(), anyList());
    }

    @Test
    void acceptingRequestCreatesParticipantAndRetainsRequest() {
        when(gameListingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(listing));
        JoinRequest request = new JoinRequest(requester, listing, Team.A);
        request.setJoinRequestId(50L);
        request.setPrimaryPositionId(5L);
        request.setAlternatePositionId(6L);
        when(joinRequestRepository
                .findFirstByGameListingGameListingIdAndUserUserIdAndStatusOrderByCreatedAtDesc(
                        10L, 2L, JoinRequestStatus.PENDING)).thenReturn(Optional.of(request));
        when(schedulingConflictService.getConflictMessageMinutes(eq(2L), any(), eq(120), isNull()))
                .thenReturn(null);
        when(gameJoinerRepository.findByUserAndListing(2L, 10L)).thenReturn(Optional.empty());
        when(gameJoinerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        GameJoiner participant = service.acceptRequest(10L, 2L, 1L);

        assertThat(participant.getStatus()).isEqualTo(JoinerStatus.ACCEPTED);
        assertThat(participant.getJoinRequest()).isSameAs(request);
        assertThat(participant.getPrimaryPositionId()).isEqualTo(5L);
        assertThat(participant.getAlternatePositionId()).isEqualTo(6L);
        assertThat(request.getStatus()).isEqualTo(JoinRequestStatus.ACCEPTED);
    }

    @Test
    void joinRequestPreservesPrimaryAndAlternatePositions() {
        listing.getFormat().setHasPositions(true);
        arrangeSuccessfulRequest();
        when(sportService.getPositionIdsForFormat(1L)).thenReturn(java.util.Set.of(5L, 6L));

        JoinRequest result = service.sendJoinRequest(2L, 10L, Team.A, 5L, 6L);

        assertThat(result.getPrimaryPositionId()).isEqualTo(5L);
        assertThat(result.getAlternatePositionId()).isEqualTo(6L);
    }

    @Test
    void requestIsRejectedAtTwoHourLockIn() {
        listing.setScheduledDate(LocalDateTime.now().plusHours(2));
        arrangeRequestStart();

        assertThatThrownBy(() -> service.sendJoinRequest(2L, 10L, Team.A, null, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("close 2 hours");
    }

    @Test
    void invitationDoesNotAutoAccept() {
        Invitation invitation = new Invitation(listing, requester);
        arrangeRequestStart();
        when(invitationRepository.findByGameListingGameListingIdAndInviteeUserId(10L, 2L))
                .thenReturn(Optional.of(invitation));
        when(schedulingConflictService.getConflictMessageMinutes(eq(2L), any(), eq(120), isNull()))
                .thenReturn(null);
        when(joinRequestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        JoinRequest result = service.sendJoinRequest(2L, 10L, Team.B, null, null);

        assertThat(result.getStatus()).isEqualTo(JoinRequestStatus.PENDING);
        assertThat(result.getInvitation()).isSameAs(invitation);
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.USED);
    }

    @Test
    void creatorCannotRequestOwnListing() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(gameListingRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> service.sendJoinRequest(1L, 10L, Team.A, null, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("creator");
    }

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
}
