package com.gameon.model.dto;

import com.gameon.model.entity.UserWarning;
import com.gameon.model.enums.ContentType;

import java.time.LocalDate;

/**
 * Read-only projection of a {@link UserWarning} strike record for admin dashboards,
 * the readable-view parity display, and user profile "Community Warnings".
 */
public record WarningView(
        Long warningId,
        Long userId,
        String username,
        String moderatorUsername,
        int strikeNumber,
        int strikesForBan,
        ContentType contentType,
        Long contentId,
        String reason,
        Long reportId,
        LocalDate warningDate
) {
    public static WarningView from(UserWarning w) {
        return new WarningView(
                w.getWarningId(),
                w.getUser() != null ? w.getUser().getUserId() : null,
                w.getUser() != null ? w.getUser().getUsername() : null,
                w.getModerator() != null ? w.getModerator().getUsername() : null,
                w.getStrikeNumber(),
                com.gameon.model.entity.User.STRIKES_FOR_BAN,
                w.getContentType(),
                w.getContentId(),
                w.getWarningReason(),
                w.getReport() != null ? w.getReport().getReportId() : null,
                w.getWarningDate()
        );
    }
}
