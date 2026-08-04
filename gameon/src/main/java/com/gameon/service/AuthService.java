package com.gameon.service;

import com.gameon.dto.auth.RegisterStep1Dto;
import com.gameon.dto.auth.RegisterStep2Dto;
import com.gameon.exception.BusinessRuleException;
import com.gameon.exception.DuplicateResourceException;
import com.gameon.model.entity.Sport;
import com.gameon.model.entity.User;
import com.gameon.model.entity.UserSportProfile;
import com.gameon.model.entity.UserSportProfileId;
import com.gameon.model.enums.UserRole;
import com.gameon.repository.SportRepository;
import com.gameon.repository.UserRepository;
import com.gameon.repository.UserSportProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service handling user authentication operations: registration, validation.
 * Login/logout handled by Spring Security's form login mechanism.
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final UserSportProfileRepository userSportProfileRepository;
    private final SportRepository sportRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       UserSportProfileRepository userSportProfileRepository,
                       SportRepository sportRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userSportProfileRepository = userSportProfileRepository;
        this.sportRepository = sportRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Checks if a username is available for registration.
     */
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    /**
     * Registers a new user (Step 1): creates the user account with encoded password.
     * Returns the created user's ID for step 2.
     */
    @Transactional
    public Long registerStep1(RegisterStep1Dto dto) {
        // Validate passwords match
        if (!dto.passwordsMatch()) {
            throw new BusinessRuleException("Passwords do not match");
        }

        // Validate username uniqueness
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new DuplicateResourceException("Username already taken: " + dto.getUsername());
        }

        // Create user with encoded password
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setUserRole(UserRole.USER);
        user.setIsActive(true);

        User savedUser = userRepository.save(user);
        logger.info("User registered successfully: {} (ID: {})", savedUser.getUsername(), savedUser.getUserId());

        return savedUser.getUserId();
    }

    /**
     * Completes registration (Step 2): adds sport profiles to the user.
     */
    @Transactional
    public void registerStep2(Long userId, RegisterStep2Dto dto) {
        // Diagnostic logging - log all received sport selections
        logger.info("===== SPORTS RECEIVED =====");
        if (dto.getSportSelections() != null) {
            for (RegisterStep2Dto.SportSelection s : dto.getSportSelections()) {
                logger.info("sportId={} skillLevel={}", s.getSportId(), s.getSkillLevel());
            }
        }

        // Filter out entries where sportId is null (unchecked checkboxes)
        List<RegisterStep2Dto.SportSelection> selectedSports =
                (dto.getSportSelections() == null)
                        ? List.of()
                        : dto.getSportSelections()
                              .stream()
                              .filter(s -> s.getSportId() != null)
                              .toList();

        // Validate at least one sport was actually selected
        if (selectedSports.isEmpty()) {
            throw new BusinessRuleException("Please select at least one sport");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("User not found"));

        for (RegisterStep2Dto.SportSelection selection : selectedSports) {
            // Defensive check - never pass null to repository
            if (selection.getSportId() == null) {
                continue;
            }

            Sport sport = sportRepository.findById(selection.getSportId())
                    .orElseThrow(() -> new BusinessRuleException("Invalid sport selected"));

            // Check for duplicate sport on profile
            if (userSportProfileRepository.existsByIdUserIdAndIdSportId(userId, selection.getSportId())) {
                logger.warn("Sport {} already on user {} profile, skipping", sport.getSportName(), userId);
                continue;
            }

            UserSportProfile profile = new UserSportProfile();
            profile.setId(new UserSportProfileId(userId, selection.getSportId()));
            profile.setUser(user);
            profile.setSport(sport);
            profile.setSkillLevel(selection.getSkillLevel());
            profile.setWins(0);
            profile.setLosses(0);
            profile.setWinPercentage(0.0);

            userSportProfileRepository.save(profile);
        }

        logger.info("User {} completed registration with {} sport(s)",
                user.getUsername(), selectedSports.size());
    }

    /**
     * Full registration in one transaction (for programmatic use).
     */
    @Transactional
    public Long registerFull(RegisterStep1Dto step1, RegisterStep2Dto step2) {
        Long userId = registerStep1(step1);
        registerStep2(userId, step2);
        return userId;
    }
}
