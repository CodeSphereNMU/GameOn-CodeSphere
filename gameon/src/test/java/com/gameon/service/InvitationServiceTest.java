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
        when(invitationRepository.findByInviteeUserIdOrderByCreatedAtDesc(2L))
                .thenReturn(List.of(invitation));
    }

    @Test
    void invitationIsHiddenWhileJoinRequestIsPending() {
        when(joinRequestRepository.existsByGameListingGameListingIdAndUserUserIdAndStatus(
                10L, 2L, JoinRequestStatus.PENDING)).thenReturn(true);
        assertThat(service.getActiveInvitations(2L)).isEmpty();
    }

    @Test
    void invitationIsVisibleWhenNoRequestIsPending() {
        assertThat(service.getActiveInvitations(2L)).containsExactly(invitation);
    }

    @Test
    void usedInvitationIsNotActive() {
        invitation.setStatus(InvitationStatus.USED);
        assertThat(service.getActiveInvitations(2L)).isEmpty();
    }

    @Test
    void invitationIsHiddenAtLockIn() {
        listing.setScheduledDate(LocalDateTime.now().plusHours(2));
        assertThat(service.getActiveInvitations(2L)).isEmpty();
    }
}
