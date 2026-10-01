package com.gameon.security;

import com.gameon.model.entity.User;
import com.gameon.model.enums.AccountStatus;
import com.gameon.model.enums.UserRole;
import com.gameon.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for {@link ModerationAwareAuthenticationFailureHandler}: banned/suspended accounts
 * are redirected to a status-specific login message; ordinary bad credentials fall back to the
 * generic error. Authentication has already failed at this point, so no session is created.
 */
@ExtendWith(MockitoExtension.class)
class ModerationAwareAuthenticationFailureHandlerTest {

    @Mock UserRepository userRepository;
    ModerationAwareAuthenticationFailureHandler handler;

    private User banned;
    private User suspended;

    @BeforeEach
    void setUp() {
        handler = new ModerationAwareAuthenticationFailureHandler(userRepository);
        banned = new User("BannedGuy", "pw", UserRole.USER);
        banned.setUserId(8L);
        banned.setAccountStatus(AccountStatus.BANNED);
        suspended = new User("SuspendedGuy", "pw", UserRole.USER);
        suspended.setUserId(9L);
        suspended.setAccountStatus(AccountStatus.SUSPENDED);
        lenient().when(userRepository.findByUsername("BannedGuy")).thenReturn(Optional.of(banned));
        lenient().when(userRepository.findByUsername("SuspendedGuy")).thenReturn(Optional.of(suspended));
    }

    @Test
    void bannedAccountRedirectsToBannedMessage() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("username", "BannedGuy");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(request, response, new DisabledException("disabled"));

        assertThat(response.getRedirectedUrl()).startsWith("/login?banned");
    }

    @Test
    void suspendedAccountRedirectsToSuspendedMessage() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("username", "SuspendedGuy");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(request, response, new DisabledException("disabled"));

        assertThat(response.getRedirectedUrl()).startsWith("/login?suspended");
    }

    @Test
    void badCredentialsFallsBackToGenericError() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("username", "Someone");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(request, response, new BadCredentialsException("bad"));

        assertThat(response.getRedirectedUrl()).isEqualTo("/login?error=true");
    }
}
