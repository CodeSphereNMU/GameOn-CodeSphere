package com.gameon.service;

import com.gameon.model.entity.GameListing;
import com.gameon.model.entity.Invitation;
import com.gameon.model.entity.User;
import com.gameon.model.enums.InvitationStatus;
import com.gameon.model.enums.JoinRequestStatus;
import com.gameon.model.enums.ListingStatus;
import com.gameon.model.enums.UserRole;
import com.gameon.repository.InvitationRepository;
import com.gameon.repository.JoinRequestRepository;
import com.gameon.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.any;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @Mock InvitationRepository invitationRepository;
    @Mock UserRepository userRepository;
    @Mock JoinRequestRepository joinRequestRepository;
    @InjectMocks InvitationService service;

    private GameListing listing;
    private Invitation invitation;

    @BeforeEach
    void setUp() {
        User invitee = new User("Invitee", "password", UserRole.USER);
        invitee.setUserId(2L);
        listing = new GameListing();
        listing.setGameListingId(10L);
        listing.setScheduledDate(LocalDateTime.now().plusDays(1));
        listing.setListingStatus(ListingStatus.OPEN);
        invitation = new Invitation(listing, invitee);
    }

    @Test
    void invitationIsHiddenWhileJoinRequestIsPending() {
        when(invitationRepository.findByInviteeUserIdOrderByCreatedAtDesc(2L))
                .thenReturn(List.of(invitation));
        when(joinRequestRepository.existsByGameListingGameListingIdAndUserUserIdAndStatus(
                10L, 2L, JoinRequestStatus.PENDING)).thenReturn(true);
        assertThat(service.getActiveInvitations(2L)).isEmpty();
    }

    @Test
    void invitationIsVisibleWhenNoRequestIsPending() {
        when(invitationRepository.findByInviteeUserIdOrderByCreatedAtDesc(2L))
                .thenReturn(List.of(invitation));
        assertThat(service.getActiveInvitations(2L)).containsExactly(invitation);
    }

    @Test
    void usedInvitationIsNotActive() {
        when(invitationRepository.findByInviteeUserIdOrderByCreatedAtDesc(2L))
                .thenReturn(List.of(invitation));
        invitation.setStatus(InvitationStatus.USED);
        assertThat(service.getActiveInvitations(2L)).isEmpty();
    }

    @Test
    void invitationIsHiddenAtLockIn() {
        when(invitationRepository.findByInviteeUserIdOrderByCreatedAtDesc(2L))
                .thenReturn(List.of(invitation));
        listing.setScheduledDate(LocalDateTime.now().plusHours(2));
        assertThat(service.getActiveInvitations(2L)).isEmpty();
    }

    @Test
    void inviteeCanDeclinePendingInvitation() {
        when(invitationRepository.findByInvitationIdAndInviteeUserId(55L, 2L))
                .thenReturn(java.util.Optional.of(invitation));

        service.declineInvitation(55L, 2L);

        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.DECLINED);
        verify(invitationRepository).save(invitation);
        verifyNoInteractions(joinRequestRepository);
    }

    @Test
    void invitationCannotBeDeclinedByAnotherUser() {
        when(invitationRepository.findByInvitationIdAndInviteeUserId(55L, 99L))
                .thenReturn(java.util.Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.declineInvitation(55L, 99L))
                .isInstanceOf(com.gameon.exception.BusinessRuleException.class);
        verify(invitationRepository, never()).save(any());
    }
}
