package com.gameon.service;

import com.gameon.model.entity.*;
import com.gameon.model.enums.PrivacySetting;
import com.gameon.model.enums.SkillLevel;
import com.gameon.model.enums.UserRole;
import com.gameon.repository.GameJoinerRepository;
import com.gameon.repository.GameListingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SchedulingConflictService Tests")
class SchedulingConflictServiceTest {

    @Mock
    private GameListingRepository gameListingRepository;

    @Mock
    private GameJoinerRepository gameJoinerRepository;

    @InjectMocks
    private SchedulingConflictService schedulingConflictService;

    private User testUser;
    private Sport testSport;
    private SportFormat testFormat;

    @BeforeEach
    void setUp() {
        testUser = new User("TestUser", "password", UserRole.USER);
        testUser.setUserId(1L);

        testSport = new Sport("Football", 22);
        testSport.setSportId(1L);

        testFormat = new SportFormat("5v5", 10, true, testSport);
        testFormat.setFormatId(1L);
    }

    private GameListing createListing(Long id, LocalDateTime start, int duration) {
        GameListing listing = new GameListing(testUser, testFormat, SkillLevel.INTERMEDIATE,
                start, "Test Location", PrivacySetting.PUBLIC, duration);
        listing.setGameListingId(id);
        return listing;
    }

    @Nested
    @DisplayName("Non-conflicting scenarios")
    class NonConflicting {

        @Test
        @DisplayName("User can participate in multiple non-conflicting listings")
        void noConflict_nonOverlappingSessions() {
            // Existing: 10:00 - 12:00 (2hrs) + 60min buffer = blocked until 13:00
            GameListing existing = createListing(1L,
                    LocalDateTime.of(2026, 9, 1, 10, 0), 2);

            when(gameListingRepository.findUpcomingListingsForUserAfter(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(List.of(existing));

            // New: 14:00 - 15:00 (1hr) - starts well after 13:00
            Optional<GameListing> conflict = schedulingConflictService.findSchedulingConflict(
                    1L, LocalDateTime.of(2026, 9, 1, 14, 0), 1);

            assertThat(conflict).isEmpty();
        }

        @Test
        @DisplayName("Exactly 60 minutes after previous session ends is allowed")
        void noConflict_exactlyAtBufferEnd() {
            // Existing: 10:00 - 11:00 (1hr) + 60min buffer = blocked until 12:00
            GameListing existing = createListing(1L,
                    LocalDateTime.of(2026, 9, 1, 10, 0), 1);

            when(gameListingRepository.findUpcomingListingsForUserAfter(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(List.of(existing));

            // New: 12:00 - 13:00 (1hr) - starts exactly when buffer ends
            Optional<GameListing> conflict = schedulingConflictService.findSchedulingConflict(
                    1L, LocalDateTime.of(2026, 9, 1, 12, 0), 1);

            assertThat(conflict).isEmpty();
        }

        @Test
        @DisplayName("No existing listings means no conflict")
        void noConflict_noExistingListings() {
            when(gameListingRepository.findUpcomingListingsForUserAfter(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            Optional<GameListing> conflict = schedulingConflictService.findSchedulingConflict(
                    1L, LocalDateTime.of(2026, 9, 1, 10, 0), 2);

            assertThat(conflict).isEmpty();
        }
    }

    @Nested
    @DisplayName("Conflicting scenarios")
    class Conflicting {

        @Test
        @DisplayName("Cannot participate in overlapping listings")
        void conflict_overlappingSessions() {
            // Existing: 10:00 - 12:00 (2hrs) + 60min buffer = blocked until 13:00
            GameListing existing = createListing(1L,
                    LocalDateTime.of(2026, 9, 1, 10, 0), 2);

            when(gameListingRepository.findUpcomingListingsForUserAfter(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(List.of(existing));

            // New: 11:00 - 12:00 (1hr) - overlaps existing session
            Optional<GameListing> conflict = schedulingConflictService.findSchedulingConflict(
                    1L, LocalDateTime.of(2026, 9, 1, 11, 0), 1);

            assertThat(conflict).isPresent();
        }

        @Test
        @DisplayName("Cannot create listing inside 60-minute travel buffer")
        void conflict_withinTravelBuffer() {
            // Existing: 10:00 - 12:00 (2hrs) + 60min buffer = blocked until 13:00
            GameListing existing = createListing(1L,
                    LocalDateTime.of(2026, 9, 1, 10, 0), 2);

            when(gameListingRepository.findUpcomingListingsForUserAfter(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(List.of(existing));

            // New: 12:30 - 13:30 (1hr) - starts within the 60min buffer
            Optional<GameListing> conflict = schedulingConflictService.findSchedulingConflict(
                    1L, LocalDateTime.of(2026, 9, 1, 12, 30), 1);

            assertThat(conflict).isPresent();
        }

        @Test
        @DisplayName("Conflict detection works in both directions - new before existing")
        void conflict_reverseDirection() {
            // Existing: 14:00 - 16:00 (2hrs) + 60min buffer = blocked until 17:00
            GameListing existing = createListing(1L,
                    LocalDateTime.of(2026, 9, 1, 14, 0), 2);

            when(gameListingRepository.findUpcomingListingsForUserAfter(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(List.of(existing));

            // New: 13:00 - 14:00 (1hr) + 60min buffer = blocked until 15:00
            // existing starts at 14:00, which is before newBlockedUntil (15:00) AND
            // existingEnd (16:00) is after newStart (13:00) -> conflict
            Optional<GameListing> conflict = schedulingConflictService.findSchedulingConflict(
                    1L, LocalDateTime.of(2026, 9, 1, 13, 0), 1);

            assertThat(conflict).isPresent();
        }

        @Test
        @DisplayName("Reject listing from 16:30 when existing is 14:00-16:00 plus buffer")
        void conflict_exampleFromRequirements() {
            // Existing: 14:00 - 16:00 (2hrs) + 60min buffer = blocked until 17:00
            GameListing existing = createListing(1L,
                    LocalDateTime.of(2026, 9, 1, 14, 0), 2);

            when(gameListingRepository.findUpcomingListingsForUserAfter(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(List.of(existing));

            // New: 16:30 - 17:30 (1hr) - starts within the blocked period (before 17:00)
            Optional<GameListing> conflict = schedulingConflictService.findSchedulingConflict(
                    1L, LocalDateTime.of(2026, 9, 1, 16, 30), 1);

            assertThat(conflict).isPresent();
        }
    }

    @Nested
    @DisplayName("Exclude listing scenarios")
    class ExcludeListing {

        @Test
        @DisplayName("Excluded listing is not considered for conflict")
        void noConflict_excludedListing() {
            GameListing existing = createListing(5L,
                    LocalDateTime.of(2026, 9, 1, 10, 0), 2);

            when(gameListingRepository.findUpcomingListingsForUserAfter(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(List.of(existing));

            // Same time as existing, but excluded - should not conflict
            Optional<GameListing> conflict = schedulingConflictService.findSchedulingConflict(
                    1L, LocalDateTime.of(2026, 9, 1, 10, 0), 2, 5L);

            assertThat(conflict).isEmpty();
        }
    }
}
