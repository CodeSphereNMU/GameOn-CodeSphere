package com.gameon.security;

import com.gameon.model.entity.User;
import com.gameon.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom UserDetailsService implementation for Spring Security.
 * Loads user data from the GameOnDb users table and wraps it in CustomUserDetails.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("Attempting to load user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.warn("Authentication failed: user '{}' not found", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });

        if (!user.getIsActive()) {
            logger.warn("Authentication failed: user '{}' is deactivated", username);
            throw new UsernameNotFoundException("User account is deactivated: " + username);
        }

        logger.debug("User '{}' loaded successfully with role: {}", username, user.getUserRole());
        return new CustomUserDetails(user);
    }
}
