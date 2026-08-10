package com.codesphere.gameon.service;

import com.codesphere.gameon.dao.*;
import com.codesphere.gameon.dto.JoinRequestRequest;
import com.codesphere.gameon.dto.JoinRequestResponse;
import com.codesphere.gameon.exception.ApiException;
import com.codesphere.gameon.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JoinRequestService.
 * Uses fake DAO implementations to avoid database dependency.
 */
class JoinRequestServiceTest {

    private JoinRequestService service;
    private FakeGameListingDao fakeGameListingDao;
    private FakeGameJoinerDao fakeGameJoinerDao;
    private FakeJoinRequestDao fakeJoinRequestDao;
    private FakeInvitationDao fakeInvitationDao;
    private FakeSportDao fakeSportDao;
    private FakeSportFormatDao fakeSportFormatDao;
    private FakePositionDao fakePositionDao;
    private FakeNotificationDao fakeNotificationDao;
    private FakeDataSource fakeDataSource;

    private static final long USER_ID = 1L;
    private static final long CREATOR_ID = 99L;
    private static final long LISTING_ID = 50L;
    private static final long SPORT_ID = 3L;
    private static final long FORMAT_ID = 7L;

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 8, 1, 10, 0, 0);
    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final Clock FIXED_CLOCK = Clock.fixed(
            FIXED_NOW.atZone(ZONE).toInstant(), ZONE);

    @BeforeEach
    void setUp() {
        fakeGameListingDao = new FakeGameListingDao();
        fakeGameJoinerDao = new FakeGameJoinerDao();
        fakeJoinRequestDao = new FakeJoinRequestDao();
        fakeInvitationDao = new FakeInvitationDao();
        fakeSportDao = new FakeSportDao();
        fakeSportFormatDao = new FakeSportFormatDao();
        fakePositionDao = new FakePositionDao();
        fakeNotificationDao = new FakeNotificationDao();
        fakeDataSource = new FakeDataSource();

        service = new JoinRequestService(
                fakeDataSource, fakeGameListingDao, fakeGameJoinerDao,
                fakeJoinRequestDao, fakeInvitationDao, fakeSportDao,
                fakeSportFormatDao, fakePositionDao, fakeNotificationDao, FIXED_CLOCK);

        // Default valid state: listing exists, OPEN, far in future, non-positional format
        GameListing listing = new GameListing(LISTING_ID,
                FIXED_NOW.plusDays(7), FIXED_NOW.plusDays(7).plusHours(1),
                "OPEN", false, "University Fields", "Intermediate", CREATOR_ID, FORMAT_ID);
        fakeGameListingDao.setListing(listing);

        fakeSportFormatDao.addFormat(new SportFormat(FORMAT_ID, "3v3", false, 6, 60, SPORT_ID));
        fakeSportDao.setUserHasSport(true);
        fakeGameListingDao.setConflict(false);
        fakeGameJoinerDao.setAcceptedJoiner(false);
        fakeJoinRequestDao.setHasPending(false);
        fakeInvitationDao.setPendingInvitationId(null);
    }

    // ========================================================
    // 5.1 — Validation rule tests
    // ========================================================

    @Test
    void shouldRejectWhenCreatorJoinsOwnListing() {
        JoinRequestRequest request = validRequest();
        ApiException ex = assertThrows(ApiException.class,
                () -> service.createJoinRequest(CREATOR_ID, LISTING_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("Cannot join your own listing"));
    }

    @Test
    void shouldRejectWhenListingIsConfirmed() {
        fakeGameListingDao.getListing().setStatus("CONFIRMED");
        JoinRequestRequest request = validRequest();
        ApiException ex = assertThrows(ApiException.class,
                () -> service.createJoinRequest(USER_ID, LISTING_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("not open for join requests"));
    }

    @Test
    void shouldRejectWhenListingIsCancelledByCreator() {
        fakeGameListingDao.getListing().setStatus("CANCELLED_BY_CREATOR");
        JoinRequestRequest request = validRequest();
        ApiException ex = assertThrows(ApiException.class,
                () -> service.createJoinRequest(USER_ID, LISTING_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("not open for join requests"));
    }

    @Test
    void shouldRejectWhenListingIsCancelledBySystem() {
        fakeGameListingDao.getListing().setStatus("CANCELLED_BY_SYSTEM");
        JoinRequestRequest request = validRequest();
        ApiException ex = assertThrows(ApiException.class,
                () -> service.createJoinRequest(USER_ID, LISTING_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("not open for join requests"));
    }

    @Test
    void shouldRejectWhenListingIsCompleted() {
        fakeGameListingDao.getListing().setStatus("COMPLETED");
        JoinRequestRequest request = validRequest();
        ApiException ex = assertThrows(ApiException.class,
                () -> service.createJoinRequest(USER_ID, LISTING_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("not open for join requests"));
    }

    @Test
    void shouldRejectWhenExactlyTwoHoursBeforeStart() {
        // Gap = 7200 seconds. 7200 <= 7200 = true → rejected (lock-in boundary)
        fakeGameListingDao.getListing().setDate(FIXED_NOW.plusSeconds(7200));
        fakeGameListingDao.getListing().setEndTime(FIXED_NOW.plusSeconds(7200).plusHours(1));
        JoinRequestRequest request = validRequest();
        ApiException ex = assertThrows(ApiException.class,
                () -> service.createJoinRequest(USER_ID, LISTING_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("passed lock-in"));
    }

    @Test
    void shouldRejectWhenJustUnderTwoHoursBeforeStart() {
        // Gap = 7199 seconds. 7199 <= 7200 = true → still rejected (within lock-in)
        fakeGameListingDao.getListing().setDate(FIXED_NOW.plusSeconds(7199));
        fakeGameListingDao.getListing().setEndTime(FIXED_NOW.plusSeconds(7199).plusHours(1));
        JoinRequestRequest request = validRequest();
        ApiException ex = assertThrows(ApiException.class,
                () -> service.createJoinRequest(USER_ID, LISTING_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("passed lock-in"));
    }

    @Test
    void shouldAllowWhenMoreThanTwoHoursBeforeStart() {
        // Gap = 7201 seconds. 7201 <= 7200 = false → allowed (more than 2 hours before start)
        fakeGameListingDao.getListing().setDate(FIXED_NOW.plusSeconds(7201));
        fakeGameListingDao.getListing().setEndTime(FIXED_NOW.plusSeconds(7201).plusHours(1));
        JoinRequestRequest request = validRequest();
        JoinRequestResponse response = service.createJoinRequest(USER_ID, LISTING_ID, request);
        assertNotNull(response);
    }

    @Test
    void shouldRejectWhenSportNotOnProfileAndNoInvitation() {
        fakeSportDao.setUserHasSport(false);
        fakeInvitationDao.setPendingInvitationId(null);
        JoinRequestRequest request = validRequest();
        ApiException ex = assertThrows(ApiException.class,
                () -> service.createJoinRequest(USER_ID, LISTING_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("not on your profile"));
    }

    @Test
    void shouldAllowWhenSportNotOnProfileButHasPendingInvitation() {
        fakeSportDao.setUserHasSport(false);
        fakeInvitationDao.setPendingInvitationId(42L);
        JoinRequestRequest request = validRequest();
        JoinRequestResponse response = service.createJoinRequest(USER_ID, LISTING_ID, request);
        assertNotNull(response);
        assertTrue(response.isInvitationLinked());
    }

    @Test
    void shouldRejectNullTeam() {
        JoinRequestRequest request = validRequest();
        request.setTeam(null);
        ApiException ex = assertThrows(ApiException.class,
                () -> service.createJoinRequest(USER_ID, LISTING_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("Team selection is required"));
    }

    @Test
    void shouldRejectEmptyTeam() {
        JoinRequestRequest request = validRequest();
        request.setTeam("");
        ApiException ex = assertThrows(ApiException.class,
                () -> service.createJoinRequest(USER_ID, LISTING_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("Team selection is required"));
    }

    @Test
    void shouldRejectInvalidTeamC() {
        JoinRequestRequest request = validRequest();
        request.setTeam("C");
        ApiException ex = assertThrows(ApiException.class,
                () -> service.createJoinRequest(USER_ID, LISTING_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("Team selection is required"));
    }

    @Test
    void shouldAcceptLowercaseTeamA() {
        // "a" → trim+uppercase → "A" which is in VALID_TEAMS
        JoinRequestRequest request = validRequest();
        request.setTeam("a");
        JoinRequestResponse response = service.createJoinRequest(USER_ID, LISTING_ID, request);
        assertEquals("A", response.getTeam());
    }

    // ========================================================
    // 5.2 — Conflict and duplicate tests
    // ========================================================

    @Test
    void shouldRejectWhenSchedulingConflictExists() {
        fakeGameListingDao.setConflict(true);
        JoinRequestRequest request = validRequest();
        ApiException ex = assertThrows(ApiException.class,
                () -> service.createJoinRequest(USER_ID, LISTING_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("Scheduling conflict"));
    }

    @Test
    void shouldAllowWhenNoSchedulingConflict() {
        fakeGameListingDao.setConflict(false);
        JoinRequestRequest request = validRequest();
        JoinRequestResponse response = service.createJoinRequest(USER_ID, LISTING_ID, request);
        assertNotNull(response);
    }

    @Test
    void shouldRejectWhenAlreadyAcceptedJoiner() {
        fakeGameJoinerDao.setAcceptedJoiner(true);
        JoinRequestRequest request = validRequest();
        ApiException ex = assertThrows(ApiException.class,
                () -> service.createJoinRequest(USER_ID, LISTING_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("already a participant"));
    }

    @Test
    void shouldRejectWhenAlreadyHasPendingRequest() {
        fakeJoinRequestDao.setHasPending(true);
        JoinRequestRequest request = validRequest();
        ApiException ex = assertThrows(ApiException.class,
                () -> service.createJoinRequest(USER_ID, LISTING_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("already have a pending request"));
    }

    @Test
    void shouldAllowPreviouslyRejectedUserToSubmitNewRequest() {
        // User had a previous request that was rejected, but hasPendingRequest=false
        fakeJoinRequestDao.setHasPending(false);
        fakeGameJoinerDao.setAcceptedJoiner(false);
        JoinRequestRequest request = validRequest();
        JoinRequestResponse response = service.createJoinRequest(USER_ID, LISTING_ID, request);
        assertNotNull(response);
        assertEquals("PENDING", response.getStatus());
    }

    @Test
    void shouldAllowRequestForFullTeam() {
        // No capacity check at submission time — request is allowed even if team is full
        fakeGameListingDao.setConflict(false);
        JoinRequestRequest request = validRequest();
        JoinRequestResponse response = service.createJoinRequest(USER_ID, LISTING_ID, request);
        assertNotNull(response);
    }

    // ========================================================
    // 5.3 — Position validation tests
    // ========================================================

    @Test
    void shouldAcceptValidPositionForPositionalFormat() {
        setupPositionalFormat();
        fakePositionDao.addFormatPosition(FORMAT_ID, 10L);
        JoinRequestRequest request = validRequest();
        request.setPositionId(10L);
        request.setAlternatePositionId(null);
        JoinRequestResponse response = service.createJoinRequest(USER_ID, LISTING_ID, request);
        assertNotNull(response);
    }

    @Test
    void shouldRejectMissingPositionForPositionalFormat() {
        setupPositionalFormat();
        JoinRequestRequest request = validRequest();
        request.setPositionId(null);
        request.setAlternatePositionId(null);
        ApiException ex = assertThrows(ApiException.class,
                () -> service.createJoinRequest(USER_ID, LISTING_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("A position selection is required for this format"));
    }

    @Test
    void shouldRejectPositionNotInFormat() {
        setupPositionalFormat();
        fakePositionDao.addFormatPosition(FORMAT_ID, 10L);
        JoinRequestRequest request = validRequest();
        request.setPositionId(999L); // not in format
        ApiException ex = assertThrows(ApiException.class,
                () -> service.createJoinRequest(USER_ID, LISTING_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("does not belong to the chosen format"));
    }

    @Test
    void shouldRejectAlternatePositionNotInFormat() {
        setupPositionalFormat();
        fakePositionDao.addFormatPosition(FORMAT_ID, 10L);
        // alternate 999 is not in format
        JoinRequestRequest request = validRequest();
        request.setPositionId(10L);
        request.setAlternatePositionId(999L);
        ApiException ex = assertThrows(ApiException.class,
                () -> service.createJoinRequest(USER_ID, LISTING_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("alternate position does not belong"));
    }

    @Test
    void shouldRejectDuplicatePositionIds() {
        setupPositionalFormat();
        fakePositionDao.addFormatPosition(FORMAT_ID, 10L);
        JoinRequestRequest request = validRequest();
        request.setPositionId(10L);
        request.setAlternatePositionId(10L);
        ApiException ex = assertThrows(ApiException.class,
                () -> service.createJoinRequest(USER_ID, LISTING_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("must be different"));
    }

    @Test
    void shouldRejectAlternateWithoutPrimaryPosition() {
        setupPositionalFormat();
        fakePositionDao.addFormatPosition(FORMAT_ID, 10L);
        fakePositionDao.addFormatPosition(FORMAT_ID, 11L);
        JoinRequestRequest request = validRequest();
        request.setPositionId(null);
        request.setAlternatePositionId(11L);
        ApiException ex = assertThrows(ApiException.class,
                () -> service.createJoinRequest(USER_ID, LISTING_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("Alternate position requires a primary position"));
    }

    @Test
    void shouldStorePositionsAsNullForNonPositionalFormat() {
        // Default format is non-positional. Even if positions are submitted, they should be null.
        JoinRequestRequest request = validRequest();
        request.setPositionId(999L);
        request.setAlternatePositionId(888L);
        JoinRequestResponse response = service.createJoinRequest(USER_ID, LISTING_ID, request);
        assertNotNull(response);
        // The inserted join request should have null positions
        JoinRequest inserted = fakeJoinRequestDao.getLastInserted();
        assertNull(inserted.getPositionId());
        assertNull(inserted.getAlternatePositionId());
    }

    // ========================================================
    // 5.3b — anyPosition flag tests
    // ========================================================

    @Test
    void shouldAcceptAnyPositionTrueWithNullPositionsForPositionalFormat() {
        setupPositionalFormat();
        fakePositionDao.addFormatPosition(FORMAT_ID, 10L);
        JoinRequestRequest request = validRequest();
        request.setAnyPosition(true);
        request.setPositionId(null);
        request.setAlternatePositionId(null);
        JoinRequestResponse response = service.createJoinRequest(USER_ID, LISTING_ID, request);
        assertNotNull(response);
        // Both positions stored as null
        JoinRequest inserted = fakeJoinRequestDao.getLastInserted();
        assertNull(inserted.getPositionId());
        assertNull(inserted.getAlternatePositionId());
    }

    @Test
    void shouldRejectAnyPositionTrueWithNonNullPositionId() {
        setupPositionalFormat();
        fakePositionDao.addFormatPosition(FORMAT_ID, 10L);
        JoinRequestRequest request = validRequest();
        request.setAnyPosition(true);
        request.setPositionId(10L);
        request.setAlternatePositionId(null);
        ApiException ex = assertThrows(ApiException.class,
                () -> service.createJoinRequest(USER_ID, LISTING_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("Position IDs must be null when Any Position is selected"));
    }

    @Test
    void shouldRejectAnyPositionTrueWithNonNullAlternatePositionId() {
        setupPositionalFormat();
        fakePositionDao.addFormatPosition(FORMAT_ID, 10L);
        JoinRequestRequest request = validRequest();
        request.setAnyPosition(true);
        request.setPositionId(null);
        request.setAlternatePositionId(10L);
        ApiException ex = assertThrows(ApiException.class,
                () -> service.createJoinRequest(USER_ID, LISTING_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("Position IDs must be null when Any Position is selected"));
    }

    @Test
    void shouldRejectAnyPositionFalseWithNullPositionIdForPositionalFormat() {
        // This confirms the existing behaviour: anyPosition=false requires a specific positionId
        setupPositionalFormat();
        fakePositionDao.addFormatPosition(FORMAT_ID, 10L);
        JoinRequestRequest request = validRequest();
        request.setAnyPosition(false);
        request.setPositionId(null);
        request.setAlternatePositionId(null);
        ApiException ex = assertThrows(ApiException.class,
                () -> service.createJoinRequest(USER_ID, LISTING_ID, request));
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("A position selection is required for this format"));
    }

    @Test
    void shouldIgnoreAnyPositionFlagForNonPositionalFormat() {
        // Non-positional format ignores anyPosition entirely — positions stored as null regardless
        JoinRequestRequest request = validRequest();
        request.setAnyPosition(true);
        request.setPositionId(null);
        request.setAlternatePositionId(null);
        JoinRequestResponse response = service.createJoinRequest(USER_ID, LISTING_ID, request);
        assertNotNull(response);
        JoinRequest inserted = fakeJoinRequestDao.getLastInserted();
        assertNull(inserted.getPositionId());
        assertNull(inserted.getAlternatePositionId());
    }

    // ========================================================
    // 5.4 — Happy path and invitation linking tests
    // ========================================================

    @Test
    void shouldCreatePendingJoinRequestWithCorrectFields() {
        JoinRequestRequest request = validRequest();
        JoinRequestResponse response = service.createJoinRequest(USER_ID, LISTING_ID, request);

        assertNotNull(response);
        assertEquals("PENDING", response.getStatus());
        assertEquals(LISTING_ID, response.getGameListingId());
        assertEquals("A", response.getTeam());

        JoinRequest inserted = fakeJoinRequestDao.getLastInserted();
        assertEquals(LISTING_ID, inserted.getGameListingId());
        assertEquals(USER_ID, inserted.getUserId());
        assertEquals(FORMAT_ID, inserted.getFormatId());
        assertEquals("A", inserted.getTeam());
        assertEquals("PENDING", inserted.getStatus());
    }

    @Test
    void shouldSetInvitationIdWhenPlayerHasPendingInvitation() {
        fakeInvitationDao.setPendingInvitationId(42L);
        JoinRequestRequest request = validRequest();
        JoinRequestResponse response = service.createJoinRequest(USER_ID, LISTING_ID, request);

        assertTrue(response.isInvitationLinked());
        JoinRequest inserted = fakeJoinRequestDao.getLastInserted();
        assertEquals(Long.valueOf(42L), inserted.getInvitationId());
    }

    @Test
    void shouldSetInvitationIdNullWhenNoInvitation() {
        fakeInvitationDao.setPendingInvitationId(null);
        JoinRequestRequest request = validRequest();
        JoinRequestResponse response = service.createJoinRequest(USER_ID, LISTING_ID, request);

        assertFalse(response.isInvitationLinked());
        JoinRequest inserted = fakeJoinRequestDao.getLastInserted();
        assertNull(inserted.getInvitationId());
    }

    @Test
    void shouldReturnResponseWithAllExpectedFields() {
        fakeInvitationDao.setPendingInvitationId(42L);
        JoinRequestRequest request = validRequest();
        JoinRequestResponse response = service.createJoinRequest(USER_ID, LISTING_ID, request);

        assertEquals(1L, response.getJoinRequestId());
        assertEquals(LISTING_ID, response.getGameListingId());
        assertEquals("A", response.getTeam());
        assertEquals("PENDING", response.getStatus());
        assertTrue(response.isInvitationLinked());
    }

    @Test
    void shouldCreateNotificationForListingCreatorOnSuccess() {
        JoinRequestRequest request = validRequest();
        service.createJoinRequest(USER_ID, LISTING_ID, request);

        assertEquals(1, fakeNotificationDao.getInsertedNotifications().size());
        Notification n = fakeNotificationDao.getInsertedNotifications().get(0);
        assertEquals(CREATOR_ID, n.getRecipientId());
        assertEquals("join_request", n.getTypeOfNotification());
        assertEquals(LISTING_ID, n.getGameListingId());
        assertFalse(n.isRead());
    }

    @Test
    void shouldNotCreateNotificationWhenValidationFails() {
        // Team validation fails before any transactional work
        JoinRequestRequest request = validRequest();
        request.setTeam("C");
        assertThrows(ApiException.class,
                () -> service.createJoinRequest(USER_ID, LISTING_ID, request));
        assertTrue(fakeNotificationDao.getInsertedNotifications().isEmpty());
    }

    // ========================================================
    // Helper methods
    // ========================================================

    private JoinRequestRequest validRequest() {
        JoinRequestRequest request = new JoinRequestRequest();
        request.setTeam("A");
        request.setPositionId(null);
        request.setAlternatePositionId(null);
        return request;
    }

    private void setupPositionalFormat() {
        // Replace the default non-positional format with a positional one
        fakeSportFormatDao.addFormat(new SportFormat(FORMAT_ID, "5v5", true, 10, 60, SPORT_ID));
    }

    // ========================================================
    // Fake implementations
    // ========================================================

    private static class FakeGameListingDao extends GameListingDao {
        private GameListing listing;
        private boolean conflict = false;

        FakeGameListingDao() { super(null); }

        void setListing(GameListing listing) { this.listing = listing; }
        GameListing getListing() { return listing; }
        void setConflict(boolean conflict) { this.conflict = conflict; }

        @Override
        public Optional<GameListing> findById(long gameListingId) {
            if (listing != null && listing.getGameListingId() == gameListingId) {
                return Optional.of(listing);
            }
            return Optional.empty();
        }

        @Override
        public boolean hasSchedulingConflict(Connection conn, long userId,
                LocalDateTime proposedStart, LocalDateTime proposedEnd) {
            return conflict;
        }
    }

    private static class FakeGameJoinerDao extends GameJoinerDao {
        private boolean acceptedJoiner = false;

        FakeGameJoinerDao() { super(null); }

        void setAcceptedJoiner(boolean accepted) { this.acceptedJoiner = accepted; }

        @Override
        public boolean isAcceptedJoiner(Connection conn, long gameListingId, long userId) {
            return acceptedJoiner;
        }
    }

    private static class FakeJoinRequestDao extends JoinRequestDao {
        private boolean hasPending = false;
        private JoinRequest lastInserted;
        private long nextId = 1;

        FakeJoinRequestDao() { super(null); }

        void setHasPending(boolean hasPending) { this.hasPending = hasPending; }
        JoinRequest getLastInserted() { return lastInserted; }

        @Override
        public long insert(Connection conn, JoinRequest joinRequest) {
            lastInserted = joinRequest;
            return nextId++;
        }

        @Override
        public boolean hasPendingRequest(Connection conn, long gameListingId, long userId) {
            return hasPending;
        }
    }

    private static class FakeInvitationDao extends InvitationDao {
        private Long pendingInvitationId = null;

        FakeInvitationDao() { super(null); }

        void setPendingInvitationId(Long id) { this.pendingInvitationId = id; }

        @Override
        public Long findPendingInvitationId(Connection conn, long gameListingId, long userId) {
            return pendingInvitationId;
        }
    }

    private static class FakeSportDao extends SportDao {
        private boolean userHasSport = true;

        FakeSportDao() { super(null); }

        void setUserHasSport(boolean has) { this.userHasSport = has; }

        @Override
        public boolean userHasSport(long userId, long sportId) {
            return userHasSport;
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
    }

    private static class FakeNotificationDao extends NotificationDao {
        private final List<Notification> insertedNotifications = new ArrayList<>();

        FakeNotificationDao() { super(null); }

        List<Notification> getInsertedNotifications() { return insertedNotifications; }

        @Override
        public void insertBatch(Connection conn, List<Notification> notifications) {
            if (notifications != null) {
                insertedNotifications.addAll(notifications);
            }
        }
    }

    private static class FakeDataSource implements DataSource {
        @Override
        public Connection getConnection() {
            return new FakeConnection();
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
