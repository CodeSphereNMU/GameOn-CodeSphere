package com.gameon.service;

import com.gameon.model.entity.GameJoiner;
import com.gameon.model.entity.GameListing;
import com.gameon.model.entity.Invitation;
import com.gameon.model.entity.User;
import com.gameon.model.enums.JoinerStatus;
import com.gameon.model.enums.UserRole;
import com.gameon.repository.GameJoinerRepository;
import com.gameon.repository.InvitationRepository;
import com.gameon.repository.UserRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GameJoinerRepository gameJoinerRepository;

    @InjectMocks
    private InvitationService invitationService;

    private User invitee;
    private GameListing listing;
    private Invitation invitation;

    @BeforeEach
    void setUp() {
        invitee = new User("Invitee", "password", UserRole.USER);
        invitee.setUserId(2L);
        listing = new GameListing();
        listing.setGameListingId(10L);
        listing.setScheduledDate(LocalDateTime.now().plusDays(1));
        listing.setIsCompleted(false);
        invitation = new Invitation(listing, invitee);
        when(invitationRepository.findByInviteeUserIdOrderByCreatedAtDesc(2L))
                .thenReturn(List.of(invitation));
    }

    @Test
    void invitationIsHiddenWhileJoinRequestIsActive() {
        GameJoiner pending = new GameJoiner();
        pending.setStatus(JoinerStatus.PENDING);
        when(gameJoinerRepository.findByUserAndListing(2L, 10L)).thenReturn(Optional.of(pending));

        assertThat(invitationService.getActiveInvitations(2L)).isEmpty();
    }

    @Test
    void invitationReappearsAfterRequestIsRejected() {
        GameJoiner rejected = new GameJoiner();
        rejected.setStatus(JoinerStatus.REJECTED);
        when(gameJoinerRepository.findByUserAndListing(2L, 10L)).thenReturn(Optional.of(rejected));

        assertThat(invitationService.getActiveInvitations(2L)).containsExactly(invitation);
    }

    @Test
    void invitationIsHiddenAtLockIn() {
        listing.setScheduledDate(LocalDateTime.now().plusHours(2));

        assertThat(invitationService.getActiveInvitations(2L)).isEmpty();
    }
}
