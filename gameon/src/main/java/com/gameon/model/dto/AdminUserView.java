package com.gameon.model.dto;

import com.gameon.model.entity.User;
import com.gameon.model.enums.AccountStatus;

import java.time.LocalDateTime;

/**
 * Read-only projection of a user's moderation standing for the admin dashboard.
 * Surfaces exactly what administrators need to see: username, account status,
 * warning count, ban date and the last moderation action.
 */
public record AdminUserView(
        Long userId,
        String username,
        String role,
        AccountStatus accountStatus,
        int warningCount,
        LocalDateTime bannedAt,
        String lastModerationAction,
        LocalDateTime lastModerationAt
) {
    public static AdminUserView from(User user) {
        return new AdminUserView(
                user.getUserId(),
                user.getUsername(),
                user.getTypeOfUser() != null ? user.getTypeOfUser().name() : null,
                user.getAccountStatus(),
                user.getWarningCount(),
                user.getBannedAt(),
                user.getLastModerationAction(),
                user.getLastModerationAt()
        );
    }

    /** Strikes remaining before an automatic ban (never negative). */
    public int strikesRemaining() {
        int remaining = User.STRIKES_FOR_BAN - warningCount;
        return Math.max(remaining, 0);
    }
}
