package com.gameon.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Listens for Spring Security authentication events and logs them.
 * Logs are directed to the security audit file via logback-spring.xml
 * (com.gameon.security logger → SECURITY_FILE appender).
 */
@Component
public class SecurityEventListener {

    private static final Logger logger = LoggerFactory.getLogger(SecurityEventListener.class);

    /**
     * Logs successful authentication (login).
     */
    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Authentication auth = event.getAuthentication();
        String username = extractUsername(auth);
        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority())
                .orElse("UNKNOWN");

        logger.info("LOGIN SUCCESS | User: {} | Role: {}", username, role);
    }

    /**
     * Logs failed authentication attempts (bad credentials, locked accounts, etc.).
     */
    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        String username = event.getAuthentication().getName();
        String reason = event.getException().getClass().getSimpleName();
        String message = event.getException().getMessage();

        logger.warn("LOGIN FAILED | User: {} | Reason: {} | Detail: {}", username, reason, message);
    }

    /**
     * Logs successful logout events.
     */
    @EventListener
    public void onLogoutSuccess(LogoutSuccessEvent event) {
        Authentication auth = event.getAuthentication();
        if (auth != null) {
            String username = extractUsername(auth);
            logger.info("LOGOUT | User: {}", username);
        }
    }

    private String extractUsername(Authentication auth) {
        if (auth.getPrincipal() instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return auth.getName();
    }
}
