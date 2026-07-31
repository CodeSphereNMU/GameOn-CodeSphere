package com.codesphere.gameon.service;

import com.codesphere.gameon.dao.UserDao;
import com.codesphere.gameon.exception.ApiException;
import com.codesphere.gameon.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AuthService.
 * Uses a simple fake UserDao to avoid database dependency.
 */
class AuthServiceTest {

    private AuthService authService;
    private FakeUserDao fakeUserDao;

    @BeforeEach
    void setUp() {
        fakeUserDao = new FakeUserDao();
        authService = new AuthService(fakeUserDao);
    }

    @Test
    void shouldLoginSuccessfullyWithCorrectCredentials() {
        fakeUserDao.setUser(new User(1L, "testuser", "correctpassword", "player"));

        User result = authService.login("testuser", "correctpassword");

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals("testuser", result.getUsername());
        assertEquals("player", result.getTypeOfUser());
    }

    @Test
    void shouldRejectUnknownUsername() {
        fakeUserDao.setUser(null); // no user found

        ApiException ex = assertThrows(ApiException.class, () ->
                authService.login("nonexistent", "anypassword"));

        assertEquals(401, ex.getStatus());
        assertEquals("Invalid username or password", ex.getMessage());
    }

    @Test
    void shouldRejectIncorrectPassword() {
        fakeUserDao.setUser(new User(1L, "testuser", "correctpassword", "player"));

        ApiException ex = assertThrows(ApiException.class, () ->
                authService.login("testuser", "wrongpassword"));

        assertEquals(401, ex.getStatus());
        assertEquals("Invalid username or password", ex.getMessage());
    }

    @Test
    void shouldReturnSameErrorForUnknownUsernameAndIncorrectPassword() {
        // Unknown username
        fakeUserDao.setUser(null);
        ApiException unknownUserEx = assertThrows(ApiException.class, () ->
                authService.login("nobody", "anypassword"));

        // Incorrect password
        fakeUserDao.setUser(new User(1L, "testuser", "secret123", "player"));
        ApiException wrongPassEx = assertThrows(ApiException.class, () ->
                authService.login("testuser", "wrongpassword"));

        // Both must expose the same status and message
        assertEquals(unknownUserEx.getStatus(), wrongPassEx.getStatus());
        assertEquals(unknownUserEx.getMessage(), wrongPassEx.getMessage());
    }

    @Test
    void shouldRejectBlankUsername() {
        ApiException ex = assertThrows(ApiException.class, () ->
                authService.login("", "somepassword"));

        assertEquals(400, ex.getStatus());
        assertEquals("Username is required", ex.getMessage());
    }

    @Test
    void shouldRejectNullUsername() {
        ApiException ex = assertThrows(ApiException.class, () ->
                authService.login(null, "somepassword"));

        assertEquals(400, ex.getStatus());
        assertEquals("Username is required", ex.getMessage());
    }

    @Test
    void shouldRejectBlankPassword() {
        ApiException ex = assertThrows(ApiException.class, () ->
                authService.login("testuser", ""));

        assertEquals(400, ex.getStatus());
        assertEquals("Password is required", ex.getMessage());
    }

    @Test
    void shouldRejectNullPassword() {
        ApiException ex = assertThrows(ApiException.class, () ->
                authService.login("testuser", null));

        assertEquals(400, ex.getStatus());
        assertEquals("Password is required", ex.getMessage());
    }

    @Test
    void shouldTrimUsernameBeforeLookup() {
        fakeUserDao.setUser(new User(2L, "spaceduser", "pass123", "moderator"));

        User result = authService.login("  spaceduser  ", "pass123");

        assertNotNull(result);
        assertEquals("spaceduser", result.getUsername());
    }

    @Test
    void shouldHandleNullTypeOfUser() {
        fakeUserDao.setUser(new User(3L, "newuser", "password1", null));

        User result = authService.login("newuser", "password1");

        assertNotNull(result);
        assertNull(result.getTypeOfUser());
    }

    // ========================================================
    // Fake UserDao for isolated unit testing
    // ========================================================

    /**
     * A minimal fake that returns a configured user without database access.
     */
    private static class FakeUserDao extends UserDao {

        private User user;

        FakeUserDao() {
            super(null); // no DataSource needed for the fake
        }

        void setUser(User user) {
            this.user = user;
        }

        @Override
        public Optional<User> findByUsername(String username) {
            if (user != null && user.getUsername().equals(username)) {
                return Optional.of(user);
            }
            return Optional.empty();
        }

        @Override
        public Optional<User> findById(long userId) {
            if (user != null && user.getUserId() == userId) {
                return Optional.of(user);
            }
            return Optional.empty();
        }
    }
}
