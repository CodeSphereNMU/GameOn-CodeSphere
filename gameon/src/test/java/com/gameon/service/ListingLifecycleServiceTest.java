package com.gameon.service;

import com.gameon.model.entity.GameJoiner;
import com.gameon.model.entity.GameJoinerId;
import com.gameon.model.entity.GameListing;
import com.gameon.model.entity.Sport;
import com.gameon.model.entity.SportFormat;
import com.gameon.model.entity.User;
import com.gameon.model.enums.*;
import com.gameon.repository.GameJoinerRepository;
import com.gameon.repository.GameListingRepository;
import com.gameon.repository.InvitationRepository;
import com.gameon.repository.JoinRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
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
    @Spy @InjectMocks ListingLifecycleService service;

    private User creator;
    private GameListing listing;
    private SportFormat format;

    @BeforeEach
    void setUp() {
        creator = user(1L, "Creator");
        Sport sport = new Sport("Tennis");
        sport.setSportId(1L);
        format = new SportFormat("Doubles", 4, false, 60, sport);
        format.setFormatId(1L);
        listing = new GameListing(creator, format, SkillLevel.INTERMEDIATE,
                LocalDateTime.now().plusHours(2), "Court", PrivacySetting.PUBLIC, 60);
        listing.setGameListingId(10L);
    }

    // ========== Confirmation Window Tests ==========

    @Nested
    @DisplayName("Confirmation Window")
    class ConfirmationWindowTests {

        @Test
        @DisplayName("Confirmation unavailable before T-24h for an older listing")
        void confirmationUnavailableBeforeT24h() {
            listing.setScheduledDate(LocalDateTime.now().plusHours(30));
            doReturn(LocalDateTime.now()).when(service).currentTime();

            assertThat(service.isConfirmationWindowOpen(listing)).isFalse();
        }

        @Test
        @DisplayName("Confirmation available at exactly T-24h")
        void confirmationAvailableAtT24h() {
            LocalDateTime now = LocalDateTime.now();
            listing.setScheduledDate(now.plusHours(24));
            doReturn(now).when(service).currentTime();

            assertThat(service.isConfirmationWindowOpen(listing)).isTrue();
        }

        @Test
        @DisplayName("Confirmation available between T-24h and T-2h")
        void confirmationAvailableBetweenT24hAndT2h() {
            LocalDateTime now = LocalDateTime.now();
            listing.setScheduledDate(now.plusHours(10));
            doReturn(now).when(service).currentTime();

            assertThat(service.isConfirmationWindowOpen(listing)).isTrue();
        }

        @Test
        @DisplayName("Confirmation immediately available when listing created within 24h")
        void confirmationImmediatelyAvailableWhenCreatedWithin24h() {
            LocalDateTime now = LocalDateTime.now();
            listing.setScheduledDate(now.plusHours(5));
            doReturn(now).when(service).currentTime();

            assertThat(service.isConfirmationWindowOpen(listing)).isTrue();
        }

        @Test
        @DisplayName("Confirmation unavailable after T-1h (finalised)")
        void confirmationUnavailableAfterT1h() {
            LocalDateTime now = LocalDateTime.now();
            listing.setScheduledDate(now.plusMinutes(50));
            doReturn(now).when(service).currentTime();

            assertThat(service.isConfirmationWindowOpen(listing)).isFalse();
        }

        @Test
        @DisplayName("Confirmation unavailable for non-OPEN listings")
        void confirmationUnavailableForConfirmedListing() {
            listing.setListingStatus(ListingStatus.CONFIRMED);
            listing.setScheduledDate(LocalDateTime.now().plusHours(10));

            assertThat(service.isConfirmationWindowOpen(listing)).isFalse();
        }
    }

    // ========== T-2h Confirmation Deadline Tests ==========

    @Nested
    @DisplayName("T-2h Confirmation Deadline Processing")
    class ConfirmationDeadlineTests {

        @Test
        @DisplayName("Unconfirmed accepted participant loses their place at T-2h")
        void unconfirmedParticipantReleasedAtT2h() {
            LocalDateTime now = LocalDateTime.now();
            listing.setScheduledDate(now.plusHours(1).plusMinutes(30)); // past T-2h, before T-1h
            doReturn(now).when(service).currentTime();
            when(gameListingRepository.findById(10L)).thenReturn(Optional.of(listing));

            GameJoiner unconfirmed = makeJoiner(2L, "Player2", listing, JoinerStatus.ACCEPTED);
            when(gameJoinerRepository.findUnconfirmedAccepted(10L)).thenReturn(List.of(unconfirmed));

            service.processConfirmationDeadline(10L);

            assertThat(unconfirmed.getStatus()).isEqualTo(JoinerStatus.LEFT);
            verify(gameJoinerRepository).save(unconfirmed);
            verify(notificationService).createNotification(eq(2L), contains("released"),
                    eq(NotificationType.PLACE_RELEASED_UNCONFIRMED), isNull(), eq(listing), isNull(), isNull());
        }

        @Test
        @DisplayName("Confirmed participant retains their place at T-2h")
        void confirmedParticipantRetainsPlace() {
            LocalDateTime now = LocalDateTime.now();
            listing.setScheduledDate(now.plusHours(1).plusMinutes(30));
            doReturn(now).when(service).currentTime();
            when(gameListingRepository.findById(10L)).thenReturn(Optional.of(listing));

            // No unconfirmed participants
            when(gameJoinerRepository.findUnconfirmedAccepted(10L)).thenReturn(List.of());

            service.processConfirmationDeadline(10L);

            verify(gameJoinerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Creator not confirmed at T-2h does NOT remove the creator")
        void creatorNotRemovedAtT2h() {
            LocalDateTime now = LocalDateTime.now();
            listing.setScheduledDate(now.plusHours(1).plusMinutes(30));
            doReturn(now).when(service).currentTime();
            when(gameListingRepository.findById(10L)).thenReturn(Optional.of(listing));

            GameJoiner creatorJoiner = makeJoiner(1L, "Creator", listing, JoinerStatus.ACCEPTED);
            when(gameJoinerRepository.findUnconfirmedAccepted(10L)).thenReturn(List.of(creatorJoiner));

            service.processConfirmationDeadline(10L);

            // Creator should NOT be set to LEFT
            assertThat(creatorJoiner.getStatus()).isEqualTo(JoinerStatus.ACCEPTED);
            verify(gameJoinerRepository, never()).save(creatorJoiner);
            // But creator should receive urgent warning
            verify(notificationService).createNotification(eq(1L), contains("URGENT"),
                    eq(NotificationType.CREATOR_CONFIRMATION_URGENT), isNull(), eq(listing), isNull(), isNull());
        }

        @Test
        @DisplayName("Listing with released capacity is NOT immediately cancelled at T-2h")
        void listingNotCancelledAtT2h() {
            LocalDateTime now = LocalDateTime.now();
            listing.setScheduledDate(now.plusHours(1).plusMinutes(30));
            doReturn(now).when(service).currentTime();
            when(gameListingRepository.findById(10L)).thenReturn(Optional.of(listing));

            GameJoiner unconfirmed = makeJoiner(2L, "Player2", listing, JoinerStatus.ACCEPTED);
            when(gameJoinerRepository.findUnconfirmedAccepted(10L)).thenReturn(List.of(unconfirmed));

            service.processConfirmationDeadline(10L);

            // Listing remains OPEN — enters replacement period
            assertThat(listing.getListingStatus()).isEqualTo(ListingStatus.OPEN);
            verify(gameListingRepository, never()).save(listing);
        }

        @Test
        @DisplayName("Skip processing if listing is past T-1h")
        void skipIfPastT1h() {
            LocalDateTime now = LocalDateTime.now();
            listing.setScheduledDate(now.plusMinutes(50)); // past T-1h
            doReturn(now).when(service).currentTime();
            when(gameListingRepository.findById(10L)).thenReturn(Optional.of(listing));

            service.processConfirmationDeadline(10L);

            verify(gameJoinerRepository, never()).findUnconfirmedAccepted(anyLong());
        }
    }

    // ========== T-1h Finalisation Tests ==========

    @Nested
    @DisplayName("T-1h Finalisation")
    class FinalisationTests {

        @Test
        @DisplayName("Full confirmed + confirmed creator at T-1h produces CONFIRMED state")
        void fullConfirmedListingIsConfirmed() {
            when(gameListingRepository.findById(10L)).thenReturn(Optional.of(listing));
            GameJoiner creatorJoiner = makeJoiner(1L, "Creator", listing, JoinerStatus.CONFIRMED_ATTENDANCE);
                        when(gameJoinerRepository.findByUserAndListing(1L, 10L)).thenReturn(Optional.of(creatorJoiner));

                   List<GameJoiner> confirmed = List.of(
                    makeJoiner(1L, "Creator", listing, JoinerStatus.CONFIRMED_ATTENDANCE),
                    makeJoiner(2L, "P2", listing, JoinerStatus.CONFIRMED_ATTENDANCE),
                    makeJoiner(3L, "P3", listing, JoinerStatus.CONFIRMED_ATTENDANCE),
                    makeJoiner(4L, "P4", listing, JoinerStatus.CONFIRMED_ATTENDANCE)
            );
            when(gameJoinerRepository.findConfirmedParticipants(10L)).thenReturn(confirmed);
            service.finaliseListing(10L);
            assertThat(listing.getListingStatus()).isEqualTo(ListingStatus.CONFIRMED);
            verify(gameJoinerRepository).lockAllConfirmedJoiners(10L);
            verify(joinRequestRepository).expirePendingForListing(10L);
            verify(invitationRepository).expirePendingForListing(10L);
            verify(notificationService).createBulkNotifications(
                    argThat(ids -> ids.size() == 4), contains("confirmed"),
                    eq(NotificationType.LISTING_CONFIRMED), isNull(), eq(listing), isNull(), isNull());
        }

        @Test
        @DisplayName("Insufficient confirmed capacity at T-1h causes cancellation")
        void insufficientCapacityCancels() {
            when(gameListingRepository.findById(10L)).thenReturn(Optional.of(listing));
            when(gameJoinerRepository.findUnconfirmedAccepted(10L)).thenReturn(List.of());

            GameJoiner creatorJoiner = makeJoiner(1L, "Creator", listing, JoinerStatus.CONFIRMED_ATTENDANCE);
            when(gameJoinerRepository.findByUserAndListing(1L, 10L)).thenReturn(Optional.of(creatorJoiner));

            // Only 3 confirmed, need 4
            List<GameJoiner> confirmed = List.of(
                    makeJoiner(1L, "Creator", listing, JoinerStatus.CONFIRMED_ATTENDANCE),
                    makeJoiner(2L, "P2", listing, JoinerStatus.CONFIRMED_ATTENDANCE),
                    makeJoiner(3L, "P3", listing, JoinerStatus.CONFIRMED_ATTENDANCE)
            );
            when(gameJoinerRepository.findConfirmedParticipants(10L)).thenReturn(confirmed);
            when(joinRequestRepository.findPendingUserIds(10L)).thenReturn(List.of());
            when(invitationRepository.findByGameListingGameListingIdAndStatus(anyLong(), any()))
                    .thenReturn(List.of());

            service.finaliseListing(10L);

            assertThat(listing.getListingStatus()).isEqualTo(ListingStatus.CANCELLED_INSUFFICIENT_PLAYERS);
            verify(gameJoinerRepository, never()).lockAllConfirmedJoiners(anyLong());
        }

        @Test
        @DisplayName("Creator still unconfirmed at T-1h causes cancellation")
        void creatorUnconfirmedCancels() {
            when(gameListingRepository.findById(10L)).thenReturn(Optional.of(listing));

            // Creator is still ACCEPTED (unconfirmed) at T-1h
            GameJoiner creatorJoiner = makeJoiner(1L, "Creator", listing, JoinerStatus.ACCEPTED);
            when(gameJoinerRepository.findByUserAndListing(1L, 10L)).thenReturn(Optional.of(creatorJoiner));

            // Release remaining unconfirmed (the creator is in this list but handled specially)
            when(gameJoinerRepository.findUnconfirmedAccepted(10L)).thenReturn(List.of(creatorJoiner));

            when(gameJoinerRepository.findConfirmedParticipants(10L)).thenReturn(List.of(
                    makeJoiner(2L, "P2", listing, JoinerStatus.CONFIRMED_ATTENDANCE),
                    makeJoiner(3L, "P3", listing, JoinerStatus.CONFIRMED_ATTENDANCE),
                    makeJoiner(4L, "P4", listing, JoinerStatus.CONFIRMED_ATTENDANCE)
            ));
            when(joinRequestRepository.findPendingUserIds(10L)).thenReturn(List.of());

            service.finaliseListing(10L);

            assertThat(listing.getListingStatus()).isEqualTo(ListingStatus.CANCELLED_INSUFFICIENT_PLAYERS);
            verify(notificationService).createNotification(eq(1L), contains("did not confirm"),
                    eq(NotificationType.LISTING_CANCELLED_INSUFFICIENT_PLAYERS), isNull(), eq(listing), isNull(), isNull());
        }

        @Test
        @DisplayName("Creator can still confirm during T-2h to T-1h")
        void creatorCanConfirmDuringGracePeriod() {
            when(gameListingRepository.findById(10L)).thenReturn(Optional.of(listing));

            GameJoiner creatorJoiner = makeJoiner(1L, "Creator", listing, JoinerStatus.CONFIRMED_ATTENDANCE);
            when(gameJoinerRepository.findByUserAndListing(1L, 10L)).thenReturn(Optional.of(creatorJoiner));
            // Creator confirmed during grace period (CONFIRMED_ATTENDANCE)
              List<GameJoiner> confirmed = List.of(
                    makeJoiner(1L, "Creator", listing, JoinerStatus.CONFIRMED_ATTENDANCE),
                    makeJoiner(2L, "P2", listing, JoinerStatus.CONFIRMED_ATTENDANCE),
                    makeJoiner(3L, "P3", listing, JoinerStatus.CONFIRMED_ATTENDANCE),
                    makeJoiner(4L, "P4", listing, JoinerStatus.CONFIRMED_ATTENDANCE)
            );
            when(gameJoinerRepository.findConfirmedParticipants(10L)).thenReturn(confirmed);
            service.finaliseListing(10L);
            assertThat(listing.getListingStatus()).isEqualTo(ListingStatus.CONFIRMED);
        }

        @Test
        @DisplayName("Non-OPEN listing is skipped at finalisation")
        void nonOpenListingSkipped() {
            listing.setListingStatus(ListingStatus.CONFIRMED);
            when(gameListingRepository.findById(10L)).thenReturn(Optional.of(listing));

            service.finaliseListing(10L);

            verify(gameJoinerRepository, never()).findUnconfirmedAccepted(anyLong());
            verify(gameListingRepository, never()).save(any());
        }
    }

    // ========== Last-Call Period Tests ==========

    @Nested
    @DisplayName("Last-Call Period Detection")
    class LastCallPeriodTests {

        @Test
        @DisplayName("isInLastCallPeriod true during T-2h to T-1h")
        void trueInLastCallPeriod() {
            LocalDateTime now = LocalDateTime.now();
            listing.setScheduledDate(now.plusHours(1).plusMinutes(30)); // between T-2h and T-1h
            doReturn(now).when(service).currentTime();

            assertThat(service.isInLastCallPeriod(listing)).isTrue();
        }

        @Test
        @DisplayName("isInLastCallPeriod false before T-2h")
        void falseBeforeT2h() {
            LocalDateTime now = LocalDateTime.now();
            listing.setScheduledDate(now.plusHours(3)); // before T-2h
            doReturn(now).when(service).currentTime();

            assertThat(service.isInLastCallPeriod(listing)).isFalse();
        }

        @Test
        @DisplayName("isInLastCallPeriod false after T-1h")
        void falseAfterT1h() {
            LocalDateTime now = LocalDateTime.now();
            listing.setScheduledDate(now.plusMinutes(50)); // past T-1h
            doReturn(now).when(service).currentTime();

            assertThat(service.isInLastCallPeriod(listing)).isFalse();
        }

        @Test
        @DisplayName("isInLastCallPeriod false for non-OPEN listing")
        void falseForNonOpen() {
            listing.setListingStatus(ListingStatus.CONFIRMED);
            listing.setScheduledDate(LocalDateTime.now().plusHours(1).plusMinutes(30));

            assertThat(service.isInLastCallPeriod(listing)).isFalse();
        }
    }

    // ========== Helper Methods ==========

    private User user(Long id, String name) {
        User user = new User(name, "password", UserRole.USER);
        user.setUserId(id);
        return user;
    }

    private GameJoiner makeJoiner(Long userId, String name, GameListing listing, JoinerStatus status) {
        User user = user(userId, name);
        GameJoiner joiner = new GameJoiner(user, listing, userId % 2 == 0 ? Team.B : Team.A);
        joiner.setStatus(status);
        if (status == JoinerStatus.CONFIRMED_ATTENDANCE) {
            joiner.setAttendanceConfirmedAt(LocalDateTime.now().minusHours(1));
        }
        return joiner;
    }
}
