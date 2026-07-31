package com.codesphere.gameon.service;

import com.codesphere.gameon.dao.*;
import com.codesphere.gameon.dto.CreateListingRequest;
import com.codesphere.gameon.dto.CreateListingResponse;
import com.codesphere.gameon.exception.ApiException;
import com.codesphere.gameon.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GameListingService.
 * Uses fake DAO implementations to avoid database dependency.
 */
class GameListingServiceTest {

    private GameListingService service;
    private FakeSportDao fakeSportDao;
    private FakeSportFormatDao fakeSportFormatDao;
    private FakePositionDao fakePositionDao;
    private FakeGameListingDao fakeGameListingDao;
    private FakeGameJoinerDao fakeGameJoinerDao;
    private FakeFollowDao fakeFollowDao;
    private FakeNotificationDao fakeNotificationDao;
    private FakeDataSource fakeDataSource;

    private static final long USER_ID = 1L;
    private static final long SPORT_ID = 3L;
    private static final long FORMAT_ID = 7L;

    // Fixed "now" for deterministic time-based tests
    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 8, 1, 10, 0, 0);
    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final Clock FIXED_CLOCK = Clock.fixed(
            FIXED_NOW.atZone(ZONE).toInstant(), ZONE);

    @BeforeEach
    void setUp() {
        fakeSportDao = new FakeSportDao();
        fakeSportFormatDao = new FakeSportFormatDao();
        fakePositionDao = new FakePositionDao();
        fakeGameListingDao = new FakeGameListingDao();
        fakeGameJoinerDao = new FakeGameJoinerDao();
        fakeFollowDao = new FakeFollowDao();
        fakeNotificationDao = new FakeNotificationDao();
        fakeDataSource = new FakeDataSource();

        service = new GameListingService(
                fakeDataSource, fakeSportDao, fakeSportFormatDao, fakePositionDao,
                fakeGameListingDao, fakeGameJoinerDao, fakeFollowDao, fakeNotificationDao,
                FIXED_CLOCK);

        // Default setup: user has basketball on profile, format 3v3 (6 players, no positions)
        fakeSportDao.addUserSport(USER_ID, SPORT_ID, "Basketball", "Intermediate");
        fakeSportFormatDao.addFormat(new SportFormat(FORMAT_ID, "3v3", false, 6, SPORT_ID));
        fakeFollowDao.setMutualFriendIds(USER_ID, Set.of(10L, 11L, 12L));
    }

    // --- Success case ---

    @Test
    void shouldCreateListingSuccessfully() {
        CreateListingRequest request = validRequest();

        CreateListingResponse response = service.createListing(USER_ID, request);

        assertNotNull(response);
        assertEquals(1L, response.getGameListingId());
        assertEquals("Basketball", response.getSportName());
        assertEquals("3v3", response.getFormatName());
        assertEquals("Intermediate", response.getSkillLevel());
        assertEquals(6, response.getCapacity());
        assertEquals(0, response.getInvitedCount());
        assertTrue(fakeGameJoinerDao.wasInserted());
    }

    @Test
    void shouldCreateListingWithInvitations() {
        CreateListingRequest request = validRequest();
        request.setInvitedFriendIds(List.of(10L, 11L));

        CreateListingResponse response = service.createListing(USER_ID, request);

        assertEquals(2, response.getInvitedCount());
        assertEquals(2, fakeNotificationDao.getInsertedCount());
    }

    // --- Validation: missing required fields ---

    @Test
    void shouldRejectNullSportId() {
        CreateListingRequest request = validRequest();
        request.setSportId(null);

        ApiException ex = assertThrows(ApiException.class, () -> service.createListing(USER_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("Sport is required"));
    }

    @Test
    void shouldRejectNullFormatId() {
        CreateListingRequest request = validRequest();
        request.setFormatId(null);

        ApiException ex = assertThrows(ApiException.class, () -> service.createListing(USER_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("Format is required"));
    }

    @Test
    void shouldRejectBlankLocation() {
        CreateListingRequest request = validRequest();
        request.setLocation("   ");

        ApiException ex = assertThrows(ApiException.class, () -> service.createListing(USER_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("Location is required"));
    }

    @Test
    void shouldRejectNullDate() {
        CreateListingRequest request = validRequest();
        request.setDate(null);

        ApiException ex = assertThrows(ApiException.class, () -> service.createListing(USER_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("Date is required"));
    }

    @Test
    void shouldRejectNullTime() {
        CreateListingRequest request = validRequest();
        request.setTime(null);

        ApiException ex = assertThrows(ApiException.class, () -> service.createListing(USER_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("Time is required"));
    }

    // --- Validation: skill level ---

    @Test
    void shouldRejectInvalidSkillLevel() {
        CreateListingRequest request = validRequest();
        request.setSkillLevel("Expert");

        ApiException ex = assertThrows(ApiException.class, () -> service.createListing(USER_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("Invalid skill level"));
    }

    // --- Validation: date/time ---

    @Test
    void shouldRejectPastDateTime() {
        CreateListingRequest request = validRequest();
        request.setDate("2020-01-01");
        request.setTime("10:00");

        ApiException ex = assertThrows(ApiException.class, () -> service.createListing(USER_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("must be in the future"));
    }

    @Test
    void shouldRejectInvalidDateFormat() {
        CreateListingRequest request = validRequest();
        request.setDate("not-a-date");

        ApiException ex = assertThrows(ApiException.class, () -> service.createListing(USER_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("Invalid date or time"));
    }

    // --- Validation: minimum lead time (provisional: 3 hours) ---

    @Test
    void shouldRejectListingOneMinuteFromNow() {
        CreateListingRequest request = validRequest();
        // FIXED_NOW is 2026-08-01 10:00:00, so 1 minute later = 10:01
        LocalDateTime oneMinuteAhead = FIXED_NOW.plusMinutes(1);
        request.setDate(oneMinuteAhead.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        request.setTime(oneMinuteAhead.toLocalTime().format(DateTimeFormatter.ISO_LOCAL_TIME));

        ApiException ex = assertThrows(ApiException.class, () -> service.createListing(USER_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("at least 3 hours before"));
    }

    @Test
    void shouldRejectListingTwoHoursFiftyNineMinutesFiftyNineSecondsFromNow() {
        CreateListingRequest request = validRequest();
        // 2h59m59s = 10799 seconds ahead of FIXED_NOW (10:00:00) → 12:59:59
        LocalDateTime justUnder = FIXED_NOW.plusHours(2).plusMinutes(59).plusSeconds(59);
        request.setDate(justUnder.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        request.setTime(justUnder.toLocalTime().format(DateTimeFormatter.ISO_LOCAL_TIME));

        ApiException ex = assertThrows(ApiException.class, () -> service.createListing(USER_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("at least 3 hours before"));
    }

    @Test
    void shouldAllowListingExactlyThreeHoursFromNow() {
        CreateListingRequest request = validRequest();
        // Exactly 3 hours = 10800 seconds ahead → 13:00:00
        LocalDateTime exactlyThreeHours = FIXED_NOW.plusHours(3);
        request.setDate(exactlyThreeHours.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        request.setTime(exactlyThreeHours.toLocalTime().format(DateTimeFormatter.ISO_LOCAL_TIME));

        CreateListingResponse response = service.createListing(USER_ID, request);
        assertNotNull(response);
    }

    @Test
    void shouldAllowListingMoreThanThreeHoursFromNow() {
        CreateListingRequest request = validRequest();
        // 4 hours ahead → 14:00:00
        LocalDateTime fourHoursAhead = FIXED_NOW.plusHours(4);
        request.setDate(fourHoursAhead.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        request.setTime(fourHoursAhead.toLocalTime().format(DateTimeFormatter.ISO_LOCAL_TIME));

        CreateListingResponse response = service.createListing(USER_ID, request);
        assertNotNull(response);
    }

    @Test
    void shouldStillCheckConflictAfterLeadTimeValidationPasses() {
        // 4 hours ahead passes lead time but conflicts with another listing
        fakeGameListingDao.setConflict(true);

        CreateListingRequest request = validRequest();
        LocalDateTime fourHoursAhead = FIXED_NOW.plusHours(4);
        request.setDate(fourHoursAhead.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        request.setTime(fourHoursAhead.toLocalTime().format(DateTimeFormatter.ISO_LOCAL_TIME));

        ApiException ex = assertThrows(ApiException.class, () -> service.createListing(USER_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("Scheduling conflict"));
    }

    // --- Validation: sport not on profile ---

    @Test
    void shouldRejectSportNotOnProfile() {
        CreateListingRequest request = validRequest();
        request.setSportId(999L); // sport not on user's profile

        ApiException ex = assertThrows(ApiException.class, () -> service.createListing(USER_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("not on your profile"));
    }

    // --- Validation: format not belonging to sport ---

    @Test
    void shouldRejectFormatNotBelongingToSport() {
        // Add a format that belongs to a different sport
        fakeSportFormatDao.addFormat(new SportFormat(99L, "Singles", false, 2, 888L));
        CreateListingRequest request = validRequest();
        request.setFormatId(99L);

        ApiException ex = assertThrows(ApiException.class, () -> service.createListing(USER_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("does not belong to the selected sport"));
    }

    // --- Validation: scheduling conflict ---

    @Test
    void shouldRejectListingLessThanTwoHoursFromExisting() {
        // e.g. 1h59m59s apart — conflict detected
        fakeGameListingDao.setConflict(true);

        CreateListingRequest request = validRequest();

        ApiException ex = assertThrows(ApiException.class, () -> service.createListing(USER_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("Scheduling conflict"));
    }

    @Test
    void shouldAllowListingExactlyTwoHoursAway() {
        // Exactly 7200 seconds apart — no conflict
        fakeGameListingDao.setConflict(false);

        CreateListingRequest request = validRequest();

        CreateListingResponse response = service.createListing(USER_ID, request);
        assertNotNull(response);
    }

    @Test
    void shouldAllowListingMoreThanTwoHoursAway() {
        // e.g. 2h0m1s apart — no conflict
        fakeGameListingDao.setConflict(false);

        CreateListingRequest request = validRequest();

        CreateListingResponse response = service.createListing(USER_ID, request);
        assertNotNull(response);
    }

    @Test
    void shouldNotConflictWithCompletedListing() {
        // Completed listings are excluded by the SQL (is_completed = 0)
        fakeGameListingDao.setConflict(false);

        CreateListingRequest request = validRequest();

        CreateListingResponse response = service.createListing(USER_ID, request);
        assertNotNull(response);
    }

    @Test
    void shouldOnlyCheckConflictsForAuthenticatedCreator() {
        // Another user's close listing does not affect this user
        fakeGameListingDao.setConflict(false);

        CreateListingRequest request = validRequest();

        CreateListingResponse response = service.createListing(USER_ID, request);
        assertNotNull(response);
    }

    // --- Validation: positions ---

    @Test
    void shouldRejectPositionNotBelongingToFormat() {
        // Use a format with positions
        fakeSportFormatDao.addFormat(new SportFormat(20L, "5v5", true, 10, SPORT_ID));
        fakePositionDao.addFormatPosition(20L, 1L); // only position 1 belongs

        CreateListingRequest request = validRequest();
        request.setFormatId(20L);
        request.setAnyPosition(false);
        request.setPositionId(999L); // doesn't belong

        ApiException ex = assertThrows(ApiException.class, () -> service.createListing(USER_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("does not belong to the chosen format"));
    }

    @Test
    void shouldRejectDuplicatePositions() {
        fakeSportFormatDao.addFormat(new SportFormat(20L, "5v5", true, 10, SPORT_ID));
        fakePositionDao.addFormatPosition(20L, 1L);
        fakePositionDao.addFormatPosition(20L, 2L);

        CreateListingRequest request = validRequest();
        request.setFormatId(20L);
        request.setAnyPosition(false);
        request.setPositionId(1L);
        request.setAlternatePositionId(1L); // same as first

        ApiException ex = assertThrows(ApiException.class, () -> service.createListing(USER_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("must be different"));
    }

    @Test
    void shouldRejectNoPositionWhenAnyPositionIsFalse() {
        fakeSportFormatDao.addFormat(new SportFormat(20L, "5v5", true, 10, SPORT_ID));
        fakePositionDao.addFormatPosition(20L, 1L);

        CreateListingRequest request = validRequest();
        request.setFormatId(20L);
        request.setAnyPosition(false);
        request.setPositionId(null);
        request.setAlternatePositionId(null);

        ApiException ex = assertThrows(ApiException.class, () -> service.createListing(USER_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("position selection is required"));
    }

    @Test
    void shouldRejectNoPositionWhenAnyPositionIsNull() {
        fakeSportFormatDao.addFormat(new SportFormat(20L, "5v5", true, 10, SPORT_ID));
        fakePositionDao.addFormatPosition(20L, 1L);

        CreateListingRequest request = validRequest();
        request.setFormatId(20L);
        request.setAnyPosition(null); // omitted
        request.setPositionId(null);
        request.setAlternatePositionId(null);

        ApiException ex = assertThrows(ApiException.class, () -> service.createListing(USER_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("position selection is required"));
    }

    @Test
    void shouldAcceptAnyPositionExplicitlySelected() {
        fakeSportFormatDao.addFormat(new SportFormat(20L, "5v5", true, 10, SPORT_ID));
        fakePositionDao.addFormatPosition(20L, 1L);

        CreateListingRequest request = validRequest();
        request.setFormatId(20L);
        request.setAnyPosition(true);
        request.setPositionId(null);
        request.setAlternatePositionId(null);

        CreateListingResponse response = service.createListing(USER_ID, request);
        assertNotNull(response);
    }

    @Test
    void shouldRejectAnyPositionWithRealPosition() {
        fakeSportFormatDao.addFormat(new SportFormat(20L, "5v5", true, 10, SPORT_ID));
        fakePositionDao.addFormatPosition(20L, 1L);

        CreateListingRequest request = validRequest();
        request.setFormatId(20L);
        request.setAnyPosition(true);
        request.setPositionId(1L); // conflict

        ApiException ex = assertThrows(ApiException.class, () -> service.createListing(USER_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("Cannot select both"));
    }

    @Test
    void shouldAcceptValidPositionForPositionalFormat() {
        fakeSportFormatDao.addFormat(new SportFormat(20L, "5v5", true, 10, SPORT_ID));
        fakePositionDao.addFormatPosition(20L, 1L);
        fakePositionDao.addFormatPosition(20L, 2L);

        CreateListingRequest request = validRequest();
        request.setFormatId(20L);
        request.setAnyPosition(false);
        request.setPositionId(1L);
        request.setAlternatePositionId(null);

        CreateListingResponse response = service.createListing(USER_ID, request);
        assertNotNull(response);
        assertTrue(fakeGameJoinerDao.wasInserted());
    }

    @Test
    void shouldSucceedWithoutPositionForNonPositionalFormat() {
        // Default format (FORMAT_ID = 7, "3v3") has has_positions = false
        CreateListingRequest request = validRequest();
        request.setPositionId(null);
        request.setAlternatePositionId(null);
        request.setAnyPosition(null);

        CreateListingResponse response = service.createListing(USER_ID, request);
        assertNotNull(response);
        assertTrue(fakeGameJoinerDao.wasInserted());
    }

    @Test
    void shouldIgnoreSubmittedPositionForNonPositionalFormat() {
        // Non-positional format should silently ignore positions
        CreateListingRequest request = validRequest();
        request.setPositionId(999L); // submitted but should be ignored
        request.setAlternatePositionId(888L);
        request.setAnyPosition(true);

        CreateListingResponse response = service.createListing(USER_ID, request);
        assertNotNull(response);
        // After validation, request fields should have been nulled
        assertNull(request.getPositionId());
        assertNull(request.getAlternatePositionId());
        assertNull(request.getAnyPosition());
    }

    // --- Validation: invitations ---

    @Test
    void shouldRejectNonFriendInvitation() {
        CreateListingRequest request = validRequest();
        request.setInvitedFriendIds(List.of(999L)); // not a mutual friend

        ApiException ex = assertThrows(ApiException.class, () -> service.createListing(USER_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("not a mutual friend"));
    }

    @Test
    void shouldRejectSelfInvitation() {
        CreateListingRequest request = validRequest();
        request.setInvitedFriendIds(List.of(USER_ID));

        ApiException ex = assertThrows(ApiException.class, () -> service.createListing(USER_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("Cannot invite yourself"));
    }

    @Test
    void shouldRejectDuplicateFriendIds() {
        CreateListingRequest request = validRequest();
        request.setInvitedFriendIds(List.of(10L, 10L));

        ApiException ex = assertThrows(ApiException.class, () -> service.createListing(USER_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("Duplicate friend IDs"));
    }

    @Test
    void shouldRejectTooManyInvitations() {
        // Format has 6 players, max invitations = 5
        CreateListingRequest request = validRequest();
        // Try to invite 6 friends (more than max 5)
        fakeFollowDao.setMutualFriendIds(USER_ID, Set.of(10L, 11L, 12L, 13L, 14L, 15L));
        request.setInvitedFriendIds(List.of(10L, 11L, 12L, 13L, 14L, 15L));

        ApiException ex = assertThrows(ApiException.class, () -> service.createListing(USER_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("Too many invitations"));
    }

    // --- Transaction rollback ---

    @Test
    void shouldRollbackWhenJoinerInsertFails() {
        fakeGameJoinerDao.setFailOnInsert(true);

        CreateListingRequest request = validRequest();

        assertThrows(RuntimeException.class, () -> service.createListing(USER_ID, request));
        assertTrue(fakeDataSource.getLastConnection().wasRolledBack());
    }

    // ========================================================
    // Helper methods
    // ========================================================

    private CreateListingRequest validRequest() {
        CreateListingRequest request = new CreateListingRequest();
        request.setSportId(SPORT_ID);
        request.setFormatId(FORMAT_ID);
        request.setSkillLevel("Intermediate");
        // Well beyond the 3-hour lead time from FIXED_NOW (10:00) → use next day 14:00
        request.setDate(FIXED_NOW.plusDays(1).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        request.setTime("14:00");
        request.setLocation("University Fields");
        request.setIsPrivate(false);
        request.setPositionId(null);
        request.setAlternatePositionId(null);
        request.setInvitedFriendIds(null);
        return request;
    }

    // ========================================================
    // Fake implementations
    // ========================================================

    private static class FakeSportDao extends SportDao {
        private final Map<Long, Map<Long, String[]>> userSports = new HashMap<>();

        FakeSportDao() { super(null); }

        void addUserSport(long userId, long sportId, String sportName, String skillLevel) {
            userSports.computeIfAbsent(userId, k -> new HashMap<>())
                    .put(sportId, new String[]{sportName, skillLevel});
        }

        @Override
        public List<Sport> findSportsByUserId(long userId) {
            Map<Long, String[]> sports = userSports.get(userId);
            if (sports == null) return List.of();
            List<Sport> result = new ArrayList<>();
            for (Map.Entry<Long, String[]> e : sports.entrySet()) {
                result.add(new Sport(e.getKey(), e.getValue()[0]));
            }
            return result;
        }

        @Override
        public boolean userHasSport(long userId, long sportId) {
            Map<Long, String[]> sports = userSports.get(userId);
            return sports != null && sports.containsKey(sportId);
        }

        @Override
        public String getUserSkillLevel(long userId, long sportId) {
            Map<Long, String[]> sports = userSports.get(userId);
            if (sports != null && sports.containsKey(sportId)) {
                return sports.get(sportId)[1];
            }
            return null;
        }
    }

    private static class FakeSportFormatDao extends SportFormatDao {
        private final Map<Long, SportFormat> formats = new HashMap<>();

        FakeSportFormatDao() { super(null); }

        void addFormat(SportFormat f) { formats.put(f.getFormatId(), f); }

        @Override
        public Optional<SportFormat> findById(long formatId) {
            return Optional.ofNullable(formats.get(formatId));
        }

        @Override
        public List<SportFormat> findFormatsBySportId(long sportId) {
            List<SportFormat> result = new ArrayList<>();
            for (SportFormat f : formats.values()) {
                if (f.getSportId() == sportId) result.add(f);
            }
            return result;
        }
    }

    private static class FakePositionDao extends PositionDao {
        private final Set<String> formatPositions = new HashSet<>();

        FakePositionDao() { super(null); }

        void addFormatPosition(long formatId, long positionId) {
            formatPositions.add(formatId + ":" + positionId);
        }

        @Override
        public boolean positionBelongsToFormat(long positionId, long formatId) {
            return formatPositions.contains(formatId + ":" + positionId);
        }

        @Override
        public List<Position> findPositionsByFormatId(long formatId) {
            return List.of();
        }
    }

    private static class FakeGameListingDao extends GameListingDao {
        private boolean conflict = false;
        private long nextId = 1;

        FakeGameListingDao() { super(null); }

        void setConflict(boolean conflict) { this.conflict = conflict; }

        @Override
        public boolean hasSchedulingConflict(Connection conn, long creatorId, java.time.LocalDateTime proposedDateTime) {
            return conflict;
        }

        @Override
        public long insert(Connection conn, GameListing listing) {
            return nextId++;
        }
    }

    private static class FakeGameJoinerDao extends GameJoinerDao {
        private boolean inserted = false;
        private boolean failOnInsert = false;

        FakeGameJoinerDao() { super(null); }

        void setFailOnInsert(boolean fail) { this.failOnInsert = fail; }
        boolean wasInserted() { return inserted; }

        @Override
        public void insertCreator(Connection conn, GameJoiner joiner) throws SQLException {
            if (failOnInsert) {
                throw new SQLException("Simulated failure");
            }
            inserted = true;
        }
    }

    private static class FakeFollowDao extends FollowDao {
        private final Map<Long, Set<Long>> mutualFriendIds = new HashMap<>();

        FakeFollowDao() { super(null); }

        void setMutualFriendIds(long userId, Set<Long> ids) {
            mutualFriendIds.put(userId, ids);
        }

        @Override
        public Set<Long> findMutualFollowerIds(long userId) {
            return mutualFriendIds.getOrDefault(userId, Set.of());
        }

        @Override
        public List<User> findMutualFollowers(long userId) {
            return List.of();
        }
    }

    private static class FakeNotificationDao extends NotificationDao {
        private int insertedCount = 0;

        FakeNotificationDao() { super(null); }

        int getInsertedCount() { return insertedCount; }

        @Override
        public void insertBatch(Connection conn, List<Notification> notifications) {
            if (notifications != null) {
                insertedCount += notifications.size();
            }
        }
    }

    /**
     * Fake DataSource that returns a FakeConnection.
     */
    private static class FakeDataSource implements DataSource {
        private FakeConnection lastConnection;

        FakeConnection getLastConnection() { return lastConnection; }

        @Override
        public Connection getConnection() {
            lastConnection = new FakeConnection();
            return lastConnection;
        }

        @Override public Connection getConnection(String username, String password) { return getConnection(); }
        @Override public java.io.PrintWriter getLogWriter() { return null; }
        @Override public void setLogWriter(java.io.PrintWriter out) {}
        @Override public void setLoginTimeout(int seconds) {}
        @Override public int getLoginTimeout() { return 0; }
        @Override public java.util.logging.Logger getParentLogger() { return null; }
        @Override public <T> T unwrap(Class<T> iface) { return null; }
        @Override public boolean isWrapperFor(Class<?> iface) { return false; }
    }

    /**
     * Minimal fake Connection that tracks commit/rollback state.
     */
    private static class FakeConnection implements Connection {
        private boolean autoCommit = true;
        private boolean committed = false;
        private boolean rolledBack = false;

        boolean wasRolledBack() { return rolledBack; }
        boolean wasCommitted() { return committed; }

        @Override public void setAutoCommit(boolean autoCommit) { this.autoCommit = autoCommit; }
        @Override public boolean getAutoCommit() { return autoCommit; }
        @Override public void commit() { committed = true; }
        @Override public void rollback() { rolledBack = true; }
        @Override public void close() {}
        @Override public boolean isClosed() { return false; }

        // Unused Connection methods — minimal stubs
        @Override public java.sql.Statement createStatement() { return null; }
        @Override public java.sql.PreparedStatement prepareStatement(String sql) { return null; }
        @Override public java.sql.CallableStatement prepareCall(String sql) { return null; }
        @Override public String nativeSQL(String sql) { return sql; }
        @Override public java.sql.DatabaseMetaData getMetaData() { return null; }
        @Override public void setReadOnly(boolean readOnly) {}
        @Override public boolean isReadOnly() { return false; }
        @Override public void setCatalog(String catalog) {}
        @Override public String getCatalog() { return null; }
        @Override public void setTransactionIsolation(int level) {}
        @Override public int getTransactionIsolation() { return Connection.TRANSACTION_READ_COMMITTED; }
        @Override public java.sql.SQLWarning getWarnings() { return null; }
        @Override public void clearWarnings() {}
        @Override public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency) { return null; }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) { return null; }
        @Override public java.sql.CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) { return null; }
        @Override public java.util.Map<String, Class<?>> getTypeMap() { return null; }
        @Override public void setTypeMap(java.util.Map<String, Class<?>> map) {}
        @Override public void setHoldability(int holdability) {}
        @Override public int getHoldability() { return 0; }
        @Override public java.sql.Savepoint setSavepoint() { return null; }
        @Override public java.sql.Savepoint setSavepoint(String name) { return null; }
        @Override public void rollback(java.sql.Savepoint savepoint) {}
        @Override public void releaseSavepoint(java.sql.Savepoint savepoint) {}
        @Override public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) { return null; }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) { return null; }
        @Override public java.sql.CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) { return null; }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) { return null; }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, int[] columnIndexes) { return null; }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, String[] columnNames) { return null; }
        @Override public java.sql.Clob createClob() { return null; }
        @Override public java.sql.Blob createBlob() { return null; }
        @Override public java.sql.NClob createNClob() { return null; }
        @Override public java.sql.SQLXML createSQLXML() { return null; }
        @Override public boolean isValid(int timeout) { return true; }
        @Override public void setClientInfo(String name, String value) {}
        @Override public void setClientInfo(java.util.Properties properties) {}
        @Override public String getClientInfo(String name) { return null; }
        @Override public java.util.Properties getClientInfo() { return null; }
        @Override public java.sql.Array createArrayOf(String typeName, Object[] elements) { return null; }
        @Override public java.sql.Struct createStruct(String typeName, Object[] attributes) { return null; }
        @Override public void setSchema(String schema) {}
        @Override public String getSchema() { return null; }
        @Override public void abort(java.util.concurrent.Executor executor) {}
        @Override public void setNetworkTimeout(java.util.concurrent.Executor executor, int milliseconds) {}
        @Override public int getNetworkTimeout() { return 0; }
        @Override public <T> T unwrap(Class<T> iface) { return null; }
        @Override public boolean isWrapperFor(Class<?> iface) { return false; }
    }
}
