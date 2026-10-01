package com.gameon.security;

import com.gameon.model.entity.User;
import com.gameon.model.enums.AccountStatus;
import com.gameon.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import java.io.IOException;
import java.util.Optional;

/**
 * Authentication failure handler that distinguishes banned/suspended accounts from ordinary
 * bad credentials.
 *
 * <p>A non-ACTIVE account causes {@link com.gameon.security.CustomUserDetails#isEnabled()} /
 * {@link com.gameon.security.CustomUserDetails#isAccountNonLocked()} to return false, which
 * Spring Security surfaces as a {@link DisabledException} / {@link LockedException} - and only
 * after the credentials have been verified, so authentication stops before a session is created.
 * In that case we look up the account's status and redirect to a status-specific login message
 * ({@code /login?banned} or {@code /login?suspended}) instead of the generic
 * "Invalid username or password".</p>
 */
public class ModerationAwareAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Logger logger = LoggerFactory.getLogger(ModerationAwareAuthenticationFailureHandler.class);

    private final UserRepository userRepository;

    public ModerationAwareAuthenticationFailureHandler(UserRepository userRepository) {
        super("/login?error=true");
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {

        // A disabled/locked account is a banned or suspended user. Resolve which and route to
        // the appropriate status-specific message. Any lookup issue falls back to the generic error.
        if (exception instanceof DisabledException || exception instanceof LockedException) {
            String username = request.getParameter("username");
            Optional<User> maybeUser = (username != null && !username.isBlank())
                    ? userRepository.findByUsername(username)
                    : Optional.empty();

            if (maybeUser.isPresent()) {
                AccountStatus status = maybeUser.get().getAccountStatus();
                if (status == AccountStatus.BANNED) {
                    logger.info("Login blocked for banned account '{}'", username);
                    getRedirectStrategy().sendRedirect(request, response, "/login?banned&u=" + enc(username));
                    return;
                }
                if (status == AccountStatus.SUSPENDED) {
                    logger.info("Login blocked for suspended account '{}'", username);
                    getRedirectStrategy().sendRedirect(request, response, "/login?suspended&u=" + enc(username));
                    return;
                }
            }
        }

        super.onAuthenticationFailure(request, response, exception);
    }

    private String enc(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
