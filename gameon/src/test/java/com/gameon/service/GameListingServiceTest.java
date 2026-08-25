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
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
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
                    "Location", PrivacySetting.PUBLIC, 2, null, null,
                    null, null, null, null))
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
                    "Location", PrivacySetting.PUBLIC, 2, null, null,
                    null, null, null, null);

            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("Position Validation")
    class PositionValidation {

        @Test
        @DisplayName("Sport with positions and no position selected - rejected")
        void noPositionSelected_rejected() {
            LocalDateTime future = LocalDateTime.now().plusHours(5);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(sportService.getFormatById(2L)).thenReturn(testFormatWithPositions);
            when(userSportProfileRepository.existsByIdUserIdAndIdSportId(1L, 1L)).thenReturn(true);

            assertThatThrownBy(() -> gameListingService.createListing(
                    1L, 2L, SkillLevel.INTERMEDIATE, future,
                    "Location", PrivacySetting.PUBLIC, 2, Collections.emptyList(), null,
                    null, null, null, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("select at least one position");
        }

        @Test
        @DisplayName("One specific position - allowed")
        void oneSpecificPosition_allowed() {
            LocalDateTime future = LocalDateTime.now().plusHours(5);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(sportService.getFormatById(2L)).thenReturn(testFormatWithPositions);
            when(userSportProfileRepository.existsByIdUserIdAndIdSportId(1L, 1L)).thenReturn(true);
            when(sportService.getAnyPositionId()).thenReturn(1L);
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
                    "Location", PrivacySetting.PUBLIC, 2, List.of(5L), null,
                    null, null, null, null);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Multiple specific positions - allowed")
        void multipleSpecificPositions_allowed() {
            LocalDateTime future = LocalDateTime.now().plusHours(5);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(sportService.getFormatById(2L)).thenReturn(testFormatWithPositions);
            when(userSportProfileRepository.existsByIdUserIdAndIdSportId(1L, 1L)).thenReturn(true);
            when(sportService.getAnyPositionId()).thenReturn(1L);
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
                    "Location", PrivacySetting.PUBLIC, 2, List.of(5L, 6L), null,
                    null, null, null, null);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Any Position alone - allowed")
        void anyPositionAlone_allowed() {
            LocalDateTime future = LocalDateTime.now().plusHours(5);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(sportService.getFormatById(2L)).thenReturn(testFormatWithPositions);
            when(userSportProfileRepository.existsByIdUserIdAndIdSportId(1L, 1L)).thenReturn(true);
            when(sportService.getAnyPositionId()).thenReturn(1L); // "Any Position" has ID 1
            when(schedulingConflictService.getConflictMessage(eq(1L), any(), eq(2), isNull()))
                    .thenReturn(null);
            when(gameListingRepository.save(any())).thenAnswer(i -> {
                GameListing gl = i.getArgument(0);
                gl.setGameListingId(100L);
                return gl;
            });
            when(gameJoinerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Select "Any Position" (id=1) alone
            GameListing result = gameListingService.createListing(
                    1L, 2L, SkillLevel.INTERMEDIATE, future,
                    "Location", PrivacySetting.PUBLIC, 2, List.of(1L), null,
                    null, null, null, null);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Any Position + specific position - rejected")
        void anyPositionPlusSpecific_rejected() {
            LocalDateTime future = LocalDateTime.now().plusHours(5);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(sportService.getFormatById(2L)).thenReturn(testFormatWithPositions);
            when(userSportProfileRepository.existsByIdUserIdAndIdSportId(1L, 1L)).thenReturn(true);
            when(sportService.getAnyPositionId()).thenReturn(1L); // "Any Position" has ID 1

            // Select "Any Position" (1L) + a specific position (5L)
            assertThatThrownBy(() -> gameListingService.createListing(
                    1L, 2L, SkillLevel.INTERMEDIATE, future,
                    "Location", PrivacySetting.PUBLIC, 2, List.of(1L, 5L), null,
                    null, null, null, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("'Any Position' cannot be combined with specific positions");
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
                    "Location", PrivacySetting.PUBLIC, 2, null, null,
                    null, null, null, null);

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
                    "Location", PrivacySetting.PUBLIC, 2, null, null,
                    null, null, null, null))
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
                    "Location", PrivacySetting.PUBLIC, 2, null, null,
                    null, null, null, null);

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
                    "Location", PrivacySetting.PUBLIC, 2, null, null,
                    null, null, null, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Conflict message");
        }
    }
}
