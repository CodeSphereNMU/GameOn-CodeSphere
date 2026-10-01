package com.gameon.service;

import com.gameon.exception.DuplicateResourceException;
import com.gameon.exception.ResourceNotFoundException;
import com.gameon.model.entity.User;
import com.gameon.model.enums.UserRole;
import com.gameon.model.enums.AccountStatus;
import com.gameon.repository.UserRepository;
import com.gameon.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service handling core user operations.
 * Registration is handled separately by AuthService.
 */
@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final BlockService blockService;

    public UserService(UserRepository userRepository, BlockService blockService) {
        this.userRepository = userRepository;
        this.blockService = blockService;
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
        // Mutual blocking: exclude users on the current viewer's block wall (blocked either way)
        // so blocked users never appear in search results, suggestions, or invite lists.
        List<Long> blockedIds = blockService.getBlockedUserIds(SecurityUtils.getCurrentUserId());
        if (blockedIds.isEmpty()) {
            return userRepository.findByUsernameContainingIgnoreCase(query);
        }
        return userRepository.findByUsernameContainingIgnoreCaseAndUserIdNotIn(query, blockedIds);
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

    // ============================================================
    // Moderation: strikes, bans and reinstatement
    // Users are NEVER physically deleted. A ban only sets account_status = BANNED
    // and records banned_at, preserving all reports, listings, stats and history.
    // ============================================================

    /**
     * Issues a moderation strike (warning) against a user and returns the updated user.
     *
     * <p>When the user reaches {@link User#STRIKES_FOR_BAN} strikes, the account is
     * automatically banned: {@code account_status = BANNED} and {@code banned_at} is set.
     * The last moderation action and timestamp are always recorded so the standing is
     * visible in the database, readable views and admin dashboards.</p>
     *
     * @return the updated user (with the new warning count / status)
     */
    @Transactional
    public User issueStrike(Long userId, String reason) {
        User user = getUserById(userId);
        // A banned user stays banned; a strike is a no-op beyond recording the action.
        int newCount = user.getWarningCount() + 1;
        user.setWarningCount(newCount);

        String actionLabel = "Strike issued (" + newCount + "/" + User.STRIKES_FOR_BAN + ")"
                + (reason != null && !reason.isBlank() ? ": " + reason : "");

        if (newCount >= User.STRIKES_FOR_BAN && user.getAccountStatus() != AccountStatus.BANNED) {
            user.setAccountStatus(AccountStatus.BANNED);
            user.setBannedAt(LocalDateTime.now());
            actionLabel = "Banned after " + newCount + " strikes"
                    + (reason != null && !reason.isBlank() ? ": " + reason : "");
            logger.info("User {} ({}) reached {} strikes and was BANNED", userId, user.getUsername(), newCount);
        } else {
            logger.info("User {} ({}) issued strike {} of {}", userId, user.getUsername(),
                    newCount, User.STRIKES_FOR_BAN);
        }

        recordModeration(user, actionLabel);
        return userRepository.save(user);
    }

    /**
     * Reinstates a user: account_status back to ACTIVE, warning count reset to 0, ban date
     * cleared. Records the moderation action. No data is deleted.
     */
    @Transactional
    public User reinstateUser(Long userId) {
        User user = getUserById(userId);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setWarningCount(0);
        user.setBannedAt(null);
        user.setBannedByUserId(null);
        user.setBanReason(null);
        recordModeration(user, "Reinstated (status ACTIVE, strikes reset)");
        logger.info("User {} ({}) reinstated to ACTIVE with strikes reset", userId, user.getUsername());
        return userRepository.save(user);
    }

    /**
     * Suspends a user (temporary restriction) without deleting any data. Does not change the
     * warning count.
     */
    @Transactional
    public User suspendUser(Long userId, String reason) {
        User user = getUserById(userId);
        user.setAccountStatus(AccountStatus.SUSPENDED);
        // Store the moderation reason so the suspended-login message can display it.
        user.setBanReason(reason != null && !reason.isBlank() ? reason.trim() : "Account under moderation review");
        recordModeration(user, "Suspended" + (reason != null && !reason.isBlank() ? ": " + reason : ""));
        logger.info("User {} ({}) suspended", userId, user.getUsername());
        return userRepository.save(user);
    }

    /**
     * Bans a user with an explicit reason and moderator attribution (admin control that does not
     * go through the strike flow). Preserves all records.
     */
    @Transactional
    public User banUser(Long userId, Long moderatorId, String reason) {
        User user = getUserById(userId);
        String safeReason = (reason != null && !reason.isBlank()) ? reason.trim() : "Banned by administrator";
        user.setAccountStatus(AccountStatus.BANNED);
        user.setBannedAt(LocalDateTime.now());
        user.setBannedByUserId(moderatorId);
        user.setBanReason(safeReason);
        recordModeration(user, "Banned: " + safeReason);
        logger.info("User {} ({}) banned by {}: {}", userId, user.getUsername(), moderatorId, safeReason);
        return userRepository.save(user);
    }

    /**
     * Resets a user's active warning count to zero without changing their account status.
     * Permanent warning audit records are preserved. Use {@link #reinstateUser(Long)} to also
     * clear a ban and set status ACTIVE.
     */
    @Transactional
    public User resetWarningCount(Long userId) {
        User user = getUserById(userId);
        user.setWarningCount(0);
        recordModeration(user, "Warning count reset to 0");
        logger.info("User {} ({}) warning count reset to 0", userId, user.getUsername());
        return userRepository.save(user);
    }

    private void recordModeration(User user, String actionLabel) {
        user.setLastModerationAction(actionLabel);
        user.setLastModerationAt(LocalDateTime.now());
    }

    /**
     * Bans a user directly (BANNED + banned_at). Preserves all records. Prefer
     * {@link #issueStrike(Long, String)} for the standard 3-strike moderation flow.
     */
    @Transactional
    public void deactivateUser(Long userId) {
        User user = getUserById(userId);
        user.setAccountStatus(AccountStatus.BANNED);
        user.setBannedAt(LocalDateTime.now());
        recordModeration(user, "Banned by administrator");
        userRepository.save(user);
        logger.info("User {} ({}) banned", userId, user.getUsername());
    }

    /**
     * Reactivates a user (status ACTIVE) without resetting strikes. For a full reinstatement
     * that also clears strikes and ban date, use {@link #reinstateUser(Long)}.
     */
    @Transactional
    public void activateUser(Long userId) {
        User user = getUserById(userId);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setBannedAt(null);
        user.setBannedByUserId(null);
        user.setBanReason(null);
        recordModeration(user, "Reactivated by administrator");
        userRepository.save(user);
        logger.info("User {} ({}) activated", userId, user.getUsername());
    }

    /** All users ordered by username, for the admin moderation dashboard. */
    @Transactional(readOnly = true)
    public List<User> getAllUsersForModeration() {
        return userRepository.findAllByOrderByUsernameAsc();
    }

    @Transactional(readOnly = true)
    public List<User> getUsersByRole(UserRole role) {
        return userRepository.findByTypeOfUser(role);
    }

    @Transactional(readOnly = true)
    public long getActiveUserCount() {
        return userRepository.countActiveUsers(AccountStatus.ACTIVE);
    }

    /** Count users in a given account status (moderation summary dashboard). */
    @Transactional(readOnly = true)
    public long countByStatus(AccountStatus status) {
        return userRepository.countByAccountStatus(status);
    }

    @Transactional(readOnly = true)
    public boolean existsById(Long userId) {
        return userRepository.existsById(userId);
    }
}
