package com.gameon.service;

import com.gameon.exception.DuplicateResourceException;
import com.gameon.exception.ResourceNotFoundException;
import com.gameon.model.entity.User;
import com.gameon.model.enums.UserRole;
import com.gameon.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service handling core user operations.
 * Registration is handled separately by AuthService.
 */
@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    @Transactional(readOnly = true)
    public User getUserWithSportProfiles(Long userId) {
        return userRepository.findByIdWithSportProfiles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    @Transactional(readOnly = true)
    public Page<User> searchUsers(String query, Pageable pageable) {
        return userRepository.findByUsernameContainingIgnoreCase(query, pageable);
    }

    @Transactional(readOnly = true)
    public List<User> searchUsers(String query) {
        return userRepository.findByUsernameContainingIgnoreCase(query);
    }

    @Transactional
    public User updateUsername(Long userId, String newUsername) {
        User user = getUserById(userId);

        if (newUsername.equals(user.getUsername())) {
            return user;
        }

        if (userRepository.existsByUsername(newUsername)) {
            throw new DuplicateResourceException("Username already taken: " + newUsername);
        }

        user.setUsername(newUsername);
        logger.info("User {} updated username to '{}'", userId, newUsername);
        return userRepository.save(user);
    }

    @Transactional
    public void deactivateUser(Long userId) {
        User user = getUserById(userId);
        user.setIsActive(false);
        userRepository.save(user);
        logger.info("User {} ({}) deactivated", userId, user.getUsername());
    }

    @Transactional
    public void activateUser(Long userId) {
        User user = getUserById(userId);
        user.setIsActive(true);
        userRepository.save(user);
        logger.info("User {} ({}) activated", userId, user.getUsername());
    }

    @Transactional(readOnly = true)
    public List<User> getUsersByRole(UserRole role) {
        return userRepository.findByUserRole(role);
    }

    @Transactional(readOnly = true)
    public long getActiveUserCount() {
        return userRepository.countActiveUsers();
    }

    @Transactional(readOnly = true)
    public boolean existsById(Long userId) {
        return userRepository.existsById(userId);
    }
}
