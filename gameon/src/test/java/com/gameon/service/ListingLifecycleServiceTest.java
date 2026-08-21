package com.gameon.service;

import com.gameon.model.entity.GameJoiner;
import com.gameon.model.entity.GameListing;
import com.gameon.model.entity.Sport;
import com.gameon.model.entity.SportFormat;
import com.gameon.model.entity.User;
import com.gameon.model.enums.ListingStatus;
import com.gameon.model.enums.NotificationType;
import com.gameon.model.enums.PrivacySetting;
import com.gameon.model.enums.SkillLevel;
import com.gameon.model.enums.Team;
import com.gameon.model.enums.UserRole;
import com.gameon.repository.GameJoinerRepository;
import com.gameon.repository.GameListingRepository;
import com.gameon.repository.InvitationRepository;
import com.gameon.repository.JoinRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListingLifecycleServiceTest {

    @Mock GameListingRepository gameListingRepository;
    @Mock GameJoinerRepository gameJoinerRepository;
    @Mock JoinRequestRepository joinRequestRepository;
    @Mock InvitationRepository invitationRepository;
    @Mock NotificationService notificationService;
    @InjectMocks ListingLifecycleService service;

    private GameListing listing;
    private List<GameJoiner> participants;

    @BeforeEach
    void setUp() {
        User creator = user(1L, "Creator");
        Sport sport = new Sport("Tennis");
        SportFormat format = new SportFormat("Doubles", 4, false, 60, sport);
        listing = new GameListing(creator, format, SkillLevel.INTERMEDIATE,
                LocalDateTime.now().plusHours(2), "Court", PrivacySetting.PUBLIC, 60);
        listing.setGameListingId(10L);
        participants = new ArrayList<>();
        for (long id = 1; id <= 4; id++) {
            participants.add(new GameJoiner(user(id, "User" + id), listing,
                    id % 2 == 0 ? Team.B : Team.A));
        }
        when(gameListingRepository.findById(10L)).thenReturn(Optional.of(listing));
    }

    @Test
    void fullListingConfirmsLocksParticipantsAndExpiresOutstandingEntries() {
        when(gameJoinerRepository.findParticipants(10L)).thenReturn(participants);
        when(joinRequestRepository.findPendingUserIds(10L)).thenReturn(List.of(9L));
        when(invitationRepository.findByGameListingGameListingIdAndStatus(anyLong(), any()))
                .thenReturn(List.of());

        service.lockInListing(10L);

        assertThat(listing.getListingStatus()).isEqualTo(ListingStatus.CONFIRMED);
        verify(gameJoinerRepository).lockAllAcceptedJoiners(10L);
        verify(joinRequestRepository).expirePendingForListing(10L);
        verify(invitationRepository).expirePendingForListing(10L);
        verify(notificationService).createBulkNotifications(
                argThat(ids -> ids.size() == 4), anyString(),
                eq(NotificationType.LISTING_CONFIRMED), isNull(), eq(listing), isNull(), isNull());
    }

    @Test
    void insufficientListingCancelsAndExpiresOutstandingEntries() {
        when(gameJoinerRepository.findParticipants(10L)).thenReturn(participants.subList(0, 3));
        when(joinRequestRepository.findPendingUserIds(10L)).thenReturn(List.of(9L));
        when(invitationRepository.findByGameListingGameListingIdAndStatus(anyLong(), any()))
                .thenReturn(List.of());

        service.lockInListing(10L);

        assertThat(listing.getListingStatus())
                .isEqualTo(ListingStatus.CANCELLED_INSUFFICIENT_PLAYERS);
        verify(gameJoinerRepository, never()).lockAllAcceptedJoiners(anyLong());
        verify(joinRequestRepository).expirePendingForListing(10L);
        verify(invitationRepository).expirePendingForListing(10L);
        verify(notificationService).createBulkNotifications(
                argThat(ids -> ids.contains(9L)), anyString(),
                eq(NotificationType.LISTING_CANCELLED_INSUFFICIENT_PLAYERS),
                isNull(), eq(listing), isNull(), isNull());
    }

    private User user(Long id, String name) {
        User user = new User(name, "password", UserRole.USER);
        user.setUserId(id);
        return user;
    }
}
