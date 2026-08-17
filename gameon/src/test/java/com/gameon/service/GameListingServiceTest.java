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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameListingService Tests")
class GameListingServiceTest {

    @Mock
    private GameListingRepository gameListingRepository;

    @Mock
    private GameJoinerRepository gameJoinerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSportProfileRepository userSportProfileRepository;

    @Mock
    private SportService sportService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SchedulingConflictService schedulingConflictService;

    @Mock
    private InvitationService invitationService;

    @InjectMocks
    private GameListingService gameListingService;

    private User testUser;
    private Sport testSport;
    private SportFormat testFormat;
    private SportFormat testFormatWithPositions;

    @BeforeEach
    void setUp() {
        testUser = new User("TestUser", "password", UserRole.USER);
        testUser.setUserId(1L);

        testSport = new Sport("Tennis", 4);
        testSport.setSportId(1L);

        testFormat = new SportFormat("Doubles", 4, false, testSport);
        testFormat.setFormatId(1L);

        testFormatWithPositions = new SportFormat("5v5", 10, true, testSport);
        testFormatWithPositions.setFormatId(2L);
    }

    @Nested
    @DisplayName("Creation Time Validation")
    class CreationTimeValidation {

        @Test
        @DisplayName("Less than 3 hours before start - rejected")
        void createListing_lessThan3Hours_rejected() {
            LocalDateTime tooSoon = LocalDateTime.now().plusHours(2);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(sportService.getFormatById(1L)).thenReturn(testFormat);
            when(userSportProfileRepository.existsByIdUserIdAndIdSportId(1L, 1L)).thenReturn(true);

            assertThatThrownBy(() -> gameListingService.createListing(
                    1L, 1L, SkillLevel.INTERMEDIATE, tooSoon,
                    "Location", PrivacySetting.PUBLIC, 2, null, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("at least 3 hours before the start time");
        }

        @Test
        @DisplayName("Exactly 3 hours before start - allowed")
        void createListing_exactly3Hours_allowed() {
            LocalDateTime justRight = LocalDateTime.now().plusHours(3).plusMinutes(1);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(sportService.getFormatById(1L)).thenReturn(testFormat);
            when(userSportProfileRepository.existsByIdUserIdAndIdSportId(1L, 1L)).thenReturn(true);
            when(schedulingConflictService.getConflictMessage(eq(1L), any(), eq(2), isNull()))
                    .thenReturn(null);
            when(gameListingRepository.save(any())).thenAnswer(i -> {
                GameListing gl = i.getArgument(0);
                gl.setGameListingId(100L);
                return gl;
            });
            when(gameJoinerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            GameListing result = gameListingService.createListing(
                    1L, 1L, SkillLevel.INTERMEDIATE, justRight,
                    "Location", PrivacySetting.PUBLIC, 2, null, null);

            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("Position Validation")
    class PositionValidation {

        @Test
        @DisplayName("Sport with positions and no position choice submitted - rejected")
        void noPositionSelected_rejected() {
            LocalDateTime future = LocalDateTime.now().plusHours(5);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(sportService.getFormatById(2L)).thenReturn(testFormatWithPositions);
            when(userSportProfileRepository.existsByIdUserIdAndIdSportId(1L, 1L)).thenReturn(true);

            assertThatThrownBy(() -> gameListingService.createListing(
                    1L, 2L, SkillLevel.INTERMEDIATE, future,
                    "Location", PrivacySetting.PUBLIC, 2, null, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Choose Any Position");
        }

        @Test
        @DisplayName("One specific position - allowed")
        void oneSpecificPosition_allowed() {
            LocalDateTime future = LocalDateTime.now().plusHours(5);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(sportService.getFormatById(2L)).thenReturn(testFormatWithPositions);
            when(userSportProfileRepository.existsByIdUserIdAndIdSportId(1L, 1L)).thenReturn(true);
            when(sportService.getPositionIdsForFormat(2L)).thenReturn(java.util.Set.of(5L, 6L));
            when(schedulingConflictService.getConflictMessage(eq(1L), any(), eq(2), isNull()))
                    .thenReturn(null);
            when(gameListingRepository.save(any())).thenAnswer(i -> {
                GameListing gl = i.getArgument(0);
                gl.setGameListingId(100L);
                return gl;
            });
            when(gameJoinerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            GameListing result = gameListingService.createListing(
                    1L, 2L, SkillLevel.INTERMEDIATE, future,
                    "Location", PrivacySetting.PUBLIC, 2, List.of(5L), null);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Multiple specific positions - allowed")
        void multipleSpecificPositions_allowed() {
            LocalDateTime future = LocalDateTime.now().plusHours(5);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(sportService.getFormatById(2L)).thenReturn(testFormatWithPositions);
            when(userSportProfileRepository.existsByIdUserIdAndIdSportId(1L, 1L)).thenReturn(true);
            when(sportService.getPositionIdsForFormat(2L)).thenReturn(java.util.Set.of(5L, 6L));
            when(schedulingConflictService.getConflictMessage(eq(1L), any(), eq(2), isNull()))
                    .thenReturn(null);
            when(gameListingRepository.save(any())).thenAnswer(i -> {
                GameListing gl = i.getArgument(0);
                gl.setGameListingId(100L);
                return gl;
            });
            when(gameJoinerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            GameListing result = gameListingService.createListing(
                    1L, 2L, SkillLevel.INTERMEDIATE, future,
                    "Location", PrivacySetting.PUBLIC, 2, List.of(5L, 6L), null);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Any Position alone - allowed")
        void anyPositionAlone_allowed() {
            LocalDateTime future = LocalDateTime.now().plusHours(5);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(sportService.getFormatById(2L)).thenReturn(testFormatWithPositions);
            when(userSportProfileRepository.existsByIdUserIdAndIdSportId(1L, 1L)).thenReturn(true);
            when(schedulingConflictService.getConflictMessage(eq(1L), any(), eq(2), isNull()))
                    .thenReturn(null);
            when(gameListingRepository.save(any())).thenAnswer(i -> {
                GameListing gl = i.getArgument(0);
                gl.setGameListingId(100L);
                return gl;
            });
            when(gameJoinerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Empty list intentionally represents Any Position.
            GameListing result = gameListingService.createListing(
                    1L, 2L, SkillLevel.INTERMEDIATE, future,
                    "Location", PrivacySetting.PUBLIC, 2, Collections.emptyList(), null);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("More than two preferred positions - rejected")
        void moreThanTwoPositions_rejected() {
            LocalDateTime future = LocalDateTime.now().plusHours(5);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(sportService.getFormatById(2L)).thenReturn(testFormatWithPositions);
            when(userSportProfileRepository.existsByIdUserIdAndIdSportId(1L, 1L)).thenReturn(true);
            assertThatThrownBy(() -> gameListingService.createListing(
                    1L, 2L, SkillLevel.INTERMEDIATE, future,
                    "Location", PrivacySetting.PUBLIC, 2, List.of(5L, 6L, 7L), null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("no more than 2");
        }
    }

    @Nested
    @DisplayName("Sport Profile Restriction")
    class SportProfileRestriction {

        @Test
        @DisplayName("Sport in profile - allowed")
        void sportInProfile_allowed() {
            LocalDateTime future = LocalDateTime.now().plusHours(5);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(sportService.getFormatById(1L)).thenReturn(testFormat);
            when(userSportProfileRepository.existsByIdUserIdAndIdSportId(1L, 1L)).thenReturn(true);
            when(schedulingConflictService.getConflictMessage(eq(1L), any(), eq(2), isNull()))
                    .thenReturn(null);
            when(gameListingRepository.save(any())).thenAnswer(i -> {
                GameListing gl = i.getArgument(0);
                gl.setGameListingId(100L);
                return gl;
            });
            when(gameJoinerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            GameListing result = gameListingService.createListing(
                    1L, 1L, SkillLevel.INTERMEDIATE, future,
                    "Location", PrivacySetting.PUBLIC, 2, null, null);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Sport not in profile - rejected")
        void sportNotInProfile_rejected() {
            LocalDateTime future = LocalDateTime.now().plusHours(5);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(sportService.getFormatById(1L)).thenReturn(testFormat);
            when(userSportProfileRepository.existsByIdUserIdAndIdSportId(1L, 1L)).thenReturn(false);

            assertThatThrownBy(() -> gameListingService.createListing(
                    1L, 1L, SkillLevel.INTERMEDIATE, future,
                    "Location", PrivacySetting.PUBLIC, 2, null, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("not included in your sports profile");
        }
    }

    @Nested
    @DisplayName("Creator as Participant")
    class CreatorAsParticipant {

        @Test
        @DisplayName("Creator automatically becomes a participant")
        void creator_becomesParticipant() {
            LocalDateTime future = LocalDateTime.now().plusHours(5);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(sportService.getFormatById(1L)).thenReturn(testFormat);
            when(userSportProfileRepository.existsByIdUserIdAndIdSportId(1L, 1L)).thenReturn(true);
            when(schedulingConflictService.getConflictMessage(eq(1L), any(), eq(2), isNull()))
                    .thenReturn(null);
            when(gameListingRepository.save(any())).thenAnswer(i -> {
                GameListing gl = i.getArgument(0);
                gl.setGameListingId(100L);
                return gl;
            });
            when(gameJoinerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            gameListingService.createListing(
                    1L, 1L, SkillLevel.INTERMEDIATE, future,
                    "Location", PrivacySetting.PUBLIC, 2, null, null);

            // Verify GameJoiner was created for the creator with ACCEPTED status
            ArgumentCaptor<GameJoiner> joinerCaptor = ArgumentCaptor.forClass(GameJoiner.class);
            verify(gameJoinerRepository).save(joinerCaptor.capture());

            GameJoiner creatorJoiner = joinerCaptor.getValue();
            assertThat(creatorJoiner.getUser()).isEqualTo(testUser);
            assertThat(creatorJoiner.getStatus()).isEqualTo(JoinerStatus.ACCEPTED);
            assertThat(creatorJoiner.getTeam()).isEqualTo(Team.A);
        }
    }

    @Nested
    @DisplayName("Scheduling Conflict on Create")
    class SchedulingConflictOnCreate {

        @Test
        @DisplayName("Scheduling conflict detected - creation rejected")
        void schedulingConflict_rejected() {
            LocalDateTime future = LocalDateTime.now().plusHours(5);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(sportService.getFormatById(1L)).thenReturn(testFormat);
            when(userSportProfileRepository.existsByIdUserIdAndIdSportId(1L, 1L)).thenReturn(true);
            when(schedulingConflictService.getConflictMessage(eq(1L), any(), eq(2), isNull()))
                    .thenReturn("Conflict message");

            assertThatThrownBy(() -> gameListingService.createListing(
                    1L, 1L, SkillLevel.INTERMEDIATE, future,
                    "Location", PrivacySetting.PUBLIC, 2, null, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Conflict message");
        }
    }

    @Nested
    @DisplayName("A500 browse lock-in")
    class BrowseLockIn {

        @Test
        @DisplayName("Browse cutoff is two hours from now")
        void browseUsesTwoHourCutoff() {
            when(userSportProfileRepository.findDistinctSportIdsByUserId(1L)).thenReturn(List.of(1L));
            when(sportService.getFormatsBySportIds(List.of(1L))).thenReturn(List.of(testFormat));
            when(gameListingRepository.findAvailablePublicListings(anyList(), any(), eq(1L), any()))
                    .thenReturn(Page.empty());

            LocalDateTime before = LocalDateTime.now().plusHours(2).minusSeconds(1);
            gameListingService.browseAvailableListings(1L, PageRequest.of(0, 12));
            LocalDateTime after = LocalDateTime.now().plusHours(2).plusSeconds(1);

            ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(gameListingRepository).findAvailablePublicListings(anyList(), cutoff.capture(), eq(1L), any());
            assertThat(cutoff.getValue()).isBetween(before, after);
        }

        @Test
        @DisplayName("Date and hide-full filters are passed to browse query")
        void browseUsesDateAndHideFullFilters() {
            LocalDate selectedDate = LocalDate.now().plusDays(2);
            when(userSportProfileRepository.findDistinctSportIdsByUserId(1L)).thenReturn(List.of(1L));
            when(sportService.getFormatsBySportIds(List.of(1L))).thenReturn(List.of(testFormat));
            when(gameListingRepository.searchAvailablePublicListings(
                    anyList(), any(), eq(1L), isNull(), isNull(),
                    eq(selectedDate.atStartOfDay()), eq(selectedDate.plusDays(1).atStartOfDay()),
                    eq(true), any()))
                    .thenReturn(Page.empty());

            gameListingService.browseAvailableListings(
                    1L, null, null, selectedDate, true, PageRequest.of(0, 12));

            verify(gameListingRepository).searchAvailablePublicListings(
                    anyList(), any(), eq(1L), isNull(), isNull(),
                    eq(selectedDate.atStartOfDay()), eq(selectedDate.plusDays(1).atStartOfDay()),
                    eq(true), any());
        }
    }
}
