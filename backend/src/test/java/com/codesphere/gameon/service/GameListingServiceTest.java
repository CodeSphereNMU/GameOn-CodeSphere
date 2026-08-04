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
    private FakeInvitationDao fakeInvitationDao;
    private FakeDataSource fakeDataSource;

    private static final long USER_ID = 1L;
    private static final long SPORT_ID = 3L;
    private static final long FORMAT_ID = 7L;
    private static final int DURATION_MINUTES = 60;

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
        fakeInvitationDao = new FakeInvitationDao();
        fakeDataSource = new FakeDataSource();

        service = new GameListingService(
                fakeDataSource, fakeSportDao, fakeSportFormatDao, fakePositionDao,
                fakeGameListingDao, fakeGameJoinerDao, fakeFollowDao, fakeNotificationDao,
                fakeInvitationDao, FIXED_CLOCK);

        // Default: user has basketball on profile, format 3v3 (6 players, 60 min, no positions)
        fakeSportDao.addUserSport(USER_ID, SPORT_ID, "Basketball", "Intermediate");
        fakeSportFormatDao.addFormat(new SportFormat(FORMAT_ID, "3v3", false, 6, DURATION_MINUTES, SPORT_ID));
        fakeFollowDao.setMutualFriendIds(USER_ID, Set.of(10L, 11L, 12L));
    }

    // ========================================================
    // Success cases
    // ========================================================

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
        assertEquals("A", response.getTeam());
        assertEquals(0, response.getInvitedCount());
        assertNotNull(response.getEndTime());
        assertNotNull(response.getSessionWindow());
        assertTrue(fakeGameJoinerDao.wasInserted());
    }

    @Test
    void shouldCalculateEndTimeFromFormatDuration() {
        CreateListingRequest request = validRequest();
        // Start at 14:00 next day, duration 60 min → end at 15:00
        request.setDate(FIXED_NOW.plusDays(1).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        request.setTime("14:00");

        CreateListingResponse response = service.createListing(USER_ID, request);

        assertTrue(response.getEndTime().contains("15:00"));
        assertEquals("14:00\u201315:00", response.getSessionWindow());
    }

    @Test
    void shouldCalculateEndTimeFor120MinFormat() {
        // Rugby 15s Contact: 120 minutes
        fakeSportFormatDao.addFormat(new SportFormat(30L, "15s Contact", false, 30, 120, SPORT_ID));
        CreateListingRequest request = validRequest();
        request.setFormatId(30L);
        request.setTime("16:00");

        CreateListingResponse response = service.createListing(USER_ID, request);

        assertTrue(response.getEndTime().contains("18:00"));
        assertEquals("16:00\u201318:00", response.getSessionWindow());
    }

    @Test
    void shouldCreateListingWithInvitations() {
        CreateListingRequest request = validRequest();
        request.setInvitedFriendIds(List.of(10L, 11L));

        CreateListingResponse response = service.createListing(USER_ID, request);

        assertEquals(2, response.getInvitedCount());
        assertEquals(2, fakeNotificationDao.getInsertedCount());
        assertEquals(2, fakeInvitationDao.getInsertedCount());
    }

    @Test
    void shouldAllowMoreInvitationsThanCapacity() {
        // Format has 6 players. Invite 5 friends (more than capacity - 1 = 5, equal is fine)
        fakeFollowDao.setMutualFriendIds(USER_ID, Set.of(10L, 11L, 12L, 13L, 14L, 15L, 16L, 17L));
        CreateListingRequest request = validRequest();
        request.setInvitedFriendIds(List.of(10L, 11L, 12L, 13L, 14L, 15L, 16L, 17L));

        // 8 invites for a 6-player format — should succeed (no capacity limit)
        CreateListingResponse response = service.createListing(USER_ID, request);
        assertEquals(8, response.getInvitedCount());
        assertEquals(8, fakeInvitationDao.getInsertedCount());
    }

    // ========================================================
    // Team selection
    // ========================================================

    @Test
    void shouldAcceptTeamSelectionA() {
        CreateListingRequest request = validRequest();
        request.setTeam("A");
        CreateListingResponse response = service.createListing(USER_ID, request);
        assertEquals("A", response.getTeam());
    }

    @Test
    void shouldAcceptTeamSelectionB() {
        CreateListingRequest request = validRequest();
        request.setTeam("B");
        CreateListingResponse response = service.createListing(USER_ID, request);
        assertEquals("B", response.getTeam());
    }

    @Test
    void shouldRejectMissingTeamSelection() {
        CreateListingRequest request = validRequest();
        request.setTeam(null);
        ApiException ex = assertThrows(ApiException.class, () -> service.createListing(USER_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("Team selection is required"));
    }

    @Test
    void shouldRejectInvalidTeamSelection() {
        CreateListingRequest request = validRequest();
        request.setTeam("C");
        ApiException ex = assertThrows(ApiException.class, () -> service.createListing(USER_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("Invalid team"));
    }

    // ========================================================
    // Scheduling conflicts (service-level handling only)
    // NOTE: These tests verify the service correctly rejects when the DAO reports
    // a conflict and allows when it does not. They use a boolean fake DAO and do
    // NOT verify: the actual SQL overlap logic, accepted-joiner inclusion, status
    // filtering, exact time boundaries, or bidirectional calculation.
    // Those properties require database integration testing or manual verification.
    // ========================================================

    @Test
    void shouldRejectWhenDaoReportsSchedulingConflict() {
        fakeGameListingDao.setConflict(true);
        CreateListingRequest request = validRequest();
        ApiException ex = assertThrows(ApiException.class, () -> service.createListing(USER_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("Scheduling conflict"));
    }

    @Test
    void shouldAllowWhenDaoReportsNoConflict() {
        fakeGameListingDao.setConflict(false);
        CreateListingRequest request = validRequest();
        CreateListingResponse response = service.createListing(USER_ID, request);
        assertNotNull(response);
    }

    // ========================================================
    // Validation: missing required fields
    // ========================================================

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
    void shouldRejectInvalidSkillLevel() {
        CreateListingRequest request = validRequest();
        request.setSkillLevel("Expert");
        ApiException ex = assertThrows(ApiException.class, () -> service.createListing(USER_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("Invalid skill level"));
    }

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

    // ========================================================
    // Minimum lead time (3 hours)
    // ========================================================

    @Test
    void shouldRejectListingUnderThreeHoursFromNow() {
        CreateListingRequest request = validRequest();
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
        LocalDateTime exactlyThreeHours = FIXED_NOW.plusHours(3);
        request.setDate(exactlyThreeHours.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        request.setTime(exactlyThreeHours.toLocalTime().format(DateTimeFormatter.ISO_LOCAL_TIME));
        CreateListingResponse response = service.createListing(USER_ID, request);
        assertNotNull(response);
    }

    // ========================================================
    // Sport and format validation
    // ========================================================

    @Test
    void shouldRejectSportNotOnProfile() {
        CreateListingRequest request = validRequest();
        request.setSportId(999L);
        ApiException ex = assertThrows(ApiException.class, () -> service.createListing(USER_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("not on your profile"));
    }

    @Test
    void shouldRejectFormatNotBelongingToSport() {
        fakeSportFormatDao.addFormat(new SportFormat(99L, "Singles", false, 2, 60, 888L));
        CreateListingRequest request = validRequest();
        request.setFormatId(99L);
        ApiException ex = assertThrows(ApiException.class, () -> service.createListing(USER_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("does not belong to the selected sport"));
    }

    // ========================================================
    // Position validation
    // ========================================================

    @Test
    void shouldRejectPositionNotBelongingToFormat() {
        fakeSportFormatDao.addFormat(new SportFormat(20L, "5v5", true, 10, 60, SPORT_ID));
        fakePositionDao.addFormatPosition(20L, 1L);
        CreateListingRequest request = validRequest();
        request.setFormatId(20L);
        request.setAnyPosition(false);
        request.setPositionId(999L);
        ApiException ex = assertThrows(ApiException.class, () -> service.createListing(USER_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("does not belong to the chosen format"));
    }

    @Test
    void shouldRejectDuplicatePositions() {
        fakeSportFormatDao.addFormat(new SportFormat(20L, "5v5", true, 10, 60, SPORT_ID));
        fakePositionDao.addFormatPosition(20L, 1L);
        fakePositionDao.addFormatPosition(20L, 2L);
        CreateListingRequest request = validRequest();
        request.setFormatId(20L);
        request.setAnyPosition(false);
        request.setPositionId(1L);
        request.setAlternatePositionId(1L);
        ApiException ex = assertThrows(ApiException.class, () -> service.createListing(USER_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("must be different"));
    }

    @Test
    void shouldRejectNoPositionWhenRequired() {
        fakeSportFormatDao.addFormat(new SportFormat(20L, "5v5", true, 10, 60, SPORT_ID));
        fakePositionDao.addFormatPosition(20L, 1L);
        CreateListingRequest request = validRequest();
        request.setFormatId(20L);
        request.setAnyPosition(false);
        request.setPositionId(null);
        ApiException ex = assertThrows(ApiException.class, () -> service.createListing(USER_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("position selection is required"));
    }

    @Test
    void shouldAcceptAnyPositionExplicitlySelected() {
        fakeSportFormatDao.addFormat(new SportFormat(20L, "5v5", true, 10, 60, SPORT_ID));
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
        fakeSportFormatDao.addFormat(new SportFormat(20L, "5v5", true, 10, 60, SPORT_ID));
        fakePositionDao.addFormatPosition(20L, 1L);
        CreateListingRequest request = validRequest();
        request.setFormatId(20L);
        request.setAnyPosition(true);
        request.setPositionId(1L);
        ApiException ex = assertThrows(ApiException.class, () -> service.createListing(USER_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("Cannot select both"));
    }

    @Test
    void shouldAcceptValidPositionForPositionalFormat() {
        fakeSportFormatDao.addFormat(new SportFormat(20L, "5v5", true, 10, 60, SPORT_ID));
        fakePositionDao.addFormatPosition(20L, 1L);
        fakePositionDao.addFormatPosition(20L, 2L);
        CreateListingRequest request = validRequest();
        request.setFormatId(20L);
        request.setAnyPosition(false);
        request.setPositionId(1L);
        request.setAlternatePositionId(2L);
        CreateListingResponse response = service.createListing(USER_ID, request);
        assertNotNull(response);
    }

    @Test
    void shouldIgnoreSubmittedPositionForNonPositionalFormat() {
        CreateListingRequest request = validRequest();
        request.setPositionId(999L);
        request.setAlternatePositionId(888L);
        request.setAnyPosition(true);
        CreateListingResponse response = service.createListing(USER_ID, request);
        assertNotNull(response);
        assertNull(request.getPositionId());
        assertNull(request.getAlternatePositionId());
        assertNull(request.getAnyPosition());
    }

    // ========================================================
    // Invitation validation
    // ========================================================

    @Test
    void shouldRejectNonFriendInvitation() {
        CreateListingRequest request = validRequest();
        request.setInvitedFriendIds(List.of(999L));
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
    void shouldInsertInvitationRecordsInTransaction() {
        CreateListingRequest request = validRequest();
        request.setInvitedFriendIds(List.of(10L, 11L, 12L));
        CreateListingResponse response = service.createListing(USER_ID, request);
        assertEquals(3, fakeInvitationDao.getInsertedCount());
        assertEquals(3, fakeNotificationDao.getInsertedCount());
        assertEquals(3, response.getInvitedCount());
    }

    // ========================================================
    // Transaction rollback
    // ========================================================

    @Test
    void shouldRollbackWhenJoinerInsertFails() {
        fakeGameJoinerDao.setFailOnInsert(true);
        CreateListingRequest request = validRequest();
        assertThrows(RuntimeException.class, () -> service.createListing(USER_ID, request));
        assertTrue(fakeDataSource.getLastConnection().wasRolledBack());
    }

    @Test
    void shouldRollbackWhenInvitationInsertFails() {
        fakeInvitationDao.setFailOnInsert(true);
        CreateListingRequest request = validRequest();
        request.setInvitedFriendIds(List.of(10L));
        assertThrows(RuntimeException.class, () -> service.createListing(USER_ID, request));
        assertTrue(fakeDataSource.getLastConnection().wasRolledBack());
    }

    @Test
    void shouldRollbackWhenNotificationInsertFails() {
        fakeNotificationDao.setFailOnInsert(true);
        CreateListingRequest request = validRequest();
        request.setInvitedFriendIds(List.of(10L));
        assertThrows(RuntimeException.class, () -> service.createListing(USER_ID, request));
        assertTrue(fakeDataSource.getLastConnection().wasRolledBack());
    }

    @Test
    void shouldPassSelectedTeamAndPositionsToCreatorJoiner() {
        fakeSportFormatDao.addFormat(new SportFormat(20L, "5v5", true, 10, 60, SPORT_ID));
        fakePositionDao.addFormatPosition(20L, 1L);
        fakePositionDao.addFormatPosition(20L, 2L);

        CreateListingRequest request = validRequest();
        request.setFormatId(20L);
        request.setTeam("B");
        request.setAnyPosition(false);
        request.setPositionId(1L);
        request.setAlternatePositionId(2L);

        service.createListing(USER_ID, request);

        GameJoiner captured = fakeGameJoinerDao.getLastInserted();
        assertNotNull(captured);
        assertEquals("B", captured.getTeam());
        assertEquals("ACCEPTED", captured.getStatus());
        assertEquals(Long.valueOf(1L), captured.getPositionId());
        assertEquals(Long.valueOf(2L), captured.getAlternatePositionId());
        assertNull(captured.getJoinRequestId());
    }

    // NOTE: The scheduling conflict tests above use a fake DAO that returns a boolean.
    // They verify the service correctly rejects/allows based on the DAO's answer, but
    // do NOT test the actual SQL overlap logic or exact time boundaries.
    // The bidirectional session+buffer overlap SQL requires database integration testing
    // or manual verification against SQL Server.

    // ========================================================
    // Helper methods
    // ========================================================

    private CreateListingRequest validRequest() {
        CreateListingRequest request = new CreateListingRequest();
        request.setSportId(SPORT_ID);
        request.setFormatId(FORMAT_ID);
        request.setSkillLevel("Intermediate");
        request.setDate(FIXED_NOW.plusDays(1).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        request.setTime("14:00");
        request.setLocation("University Fields");
        request.setIsPrivate(false);
        request.setTeam("A");
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
        public boolean hasSchedulingConflict(Connection conn, long userId, java.time.LocalDateTime proposedStart, java.time.LocalDateTime proposedEnd) {
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
        private GameJoiner lastInserted = null;
        FakeGameJoinerDao() { super(null); }

        void setFailOnInsert(boolean fail) { this.failOnInsert = fail; }
        boolean wasInserted() { return inserted; }
        GameJoiner getLastInserted() { return lastInserted; }

        @Override
        public void insertCreator(Connection conn, GameJoiner joiner) throws SQLException {
            if (failOnInsert) {
                throw new SQLException("Simulated failure");
            }
            inserted = true;
            lastInserted = joiner;
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
        private boolean failOnInsert = false;
        FakeNotificationDao() { super(null); }

        int getInsertedCount() { return insertedCount; }
        void setFailOnInsert(boolean fail) { this.failOnInsert = fail; }

        @Override
        public void insertBatch(Connection conn, List<Notification> notifications) throws SQLException {
            if (failOnInsert) {
                throw new SQLException("Simulated notification failure");
            }
            if (notifications != null) {
                insertedCount += notifications.size();
            }
        }
    }

    private static class FakeInvitationDao extends InvitationDao {
        private int insertedCount = 0;
        private boolean failOnInsert = false;
        FakeInvitationDao() { super(null); }

        int getInsertedCount() { return insertedCount; }
        void setFailOnInsert(boolean fail) { this.failOnInsert = fail; }

        @Override
        public void insertBatch(Connection conn, List<Invitation> invitations) throws SQLException {
            if (failOnInsert) {
                throw new SQLException("Simulated invitation failure");
            }
            if (invitations != null) {
                insertedCount += invitations.size();
            }
        }
    }

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
        @Override public java.sql.Statement createStatement(int a, int b) { return null; }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, int a, int b) { return null; }
        @Override public java.sql.CallableStatement prepareCall(String sql, int a, int b) { return null; }
        @Override public java.util.Map<String, Class<?>> getTypeMap() { return null; }
        @Override public void setTypeMap(java.util.Map<String, Class<?>> map) {}
        @Override public void setHoldability(int holdability) {}
        @Override public int getHoldability() { return 0; }
        @Override public java.sql.Savepoint setSavepoint() { return null; }
        @Override public java.sql.Savepoint setSavepoint(String name) { return null; }
        @Override public void rollback(java.sql.Savepoint savepoint) {}
        @Override public void releaseSavepoint(java.sql.Savepoint savepoint) {}
        @Override public java.sql.Statement createStatement(int a, int b, int c) { return null; }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, int a, int b, int c) { return null; }
        @Override public java.sql.CallableStatement prepareCall(String sql, int a, int b, int c) { return null; }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, int k) { return null; }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, int[] i) { return null; }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, String[] n) { return null; }
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
