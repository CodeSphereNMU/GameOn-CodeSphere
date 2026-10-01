package com.gameon.service;

import com.gameon.exception.ResourceNotFoundException;
import com.gameon.model.entity.Report;
import com.gameon.model.entity.User;
import com.gameon.model.entity.UserWarning;
import com.gameon.model.enums.AccountStatus;
import com.gameon.model.enums.ContentType;
import com.gameon.model.enums.NotificationType;
import com.gameon.repository.ReportRepository;
import com.gameon.repository.UserRepository;
import com.gameon.repository.UserWarningRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Central service for the strike/warning audit system.
 *
 * <p>Every strike issued by a moderator:
 * <ol>
 *   <li>creates a permanent {@link UserWarning} audit record (never deleted),</li>
 *   <li>increments {@code users.warning_count},</li>
 *   <li>sends a {@link NotificationType#STRIKE_ISSUED} notification to the user,</li>
 *   <li>and, when the count reaches {@link User#STRIKES_FOR_BAN}, bans the account with
 *       full attribution ({@code account_status=BANNED}, {@code banned_at},
 *       {@code banned_by_user_id}, {@code ban_reason}).</li>
 * </ol>
 * No records (warnings, reports, moderator actions) are ever deleted, even after a ban.</p>
 */
@Service
public class WarningService {

    private static final Logger logger = LoggerFactory.getLogger(WarningService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final UserWarningRepository userWarningRepository;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final NotificationService notificationService;

    public WarningService(UserWarningRepository userWarningRepository,
                          UserRepository userRepository,
                          ReportRepository reportRepository,
                          NotificationService notificationService) {
        this.userWarningRepository = userWarningRepository;
        this.userRepository = userRepository;
        this.reportRepository = reportRepository;
        this.notificationService = notificationService;
    }

    /**
     * Issues a strike against {@code userId} on behalf of {@code moderatorId}, persisting a
     * permanent audit record, sending a notification, and auto-banning at the 3rd strike.
     *
     * @param userId      the user being struck
     * @param moderatorId the moderator/administrator issuing the strike
     * @param reason      human-readable reason for the strike
     * @param contentType the kind of content the strike relates to (may be {@code null} -> OTHER)
     * @param contentId   the offending content id, if any (may be {@code null})
     * @param reportId    the report that led to this strike, if any (may be {@code null})
     * @return the persisted {@link UserWarning} audit record
     */
    @Transactional
    public UserWarning issueStrike(Long userId, Long moderatorId, String reason,
                                   ContentType contentType, Long contentId, Long reportId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        User moderator = userRepository.findById(moderatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", moderatorId));

        Report report = null;
        if (reportId != null) {
            report = reportRepository.findById(reportId).orElse(null);
        }

        String safeReason = (reason == null || reason.isBlank()) ? "Community guideline violation" : reason.trim();

        // The active strike count is the source of truth for the strike number.
        int strikeNumber = user.getWarningCount() + 1;
        user.setWarningCount(strikeNumber);

        // 1. Permanent audit record.
        UserWarning warning = new UserWarning(user, moderator, safeReason,
                contentType, contentId, strikeNumber, report);
        UserWarning savedWarning = userWarningRepository.save(warning);

        // 2. Auto-ban with attribution at the threshold.
        boolean banned = false;
        if (strikeNumber >= User.STRIKES_FOR_BAN && user.getAccountStatus() != AccountStatus.BANNED) {
            user.setAccountStatus(AccountStatus.BANNED);
            user.setBannedAt(LocalDateTime.now());
            user.setBannedByUserId(moderatorId);
            user.setBanReason("Reached " + strikeNumber + " strikes: " + safeReason);
            user.setLastModerationAction("Banned after " + strikeNumber + " strikes: " + safeReason);
            banned = true;
            logger.info("User {} ({}) reached {} strikes and was BANNED by {}",
                    userId, user.getUsername(), strikeNumber, moderator.getUsername());
        } else {
            user.setLastModerationAction("Strike " + strikeNumber + "/" + User.STRIKES_FOR_BAN + ": " + safeReason);
            logger.info("Strike {}/{} issued to user {} ({}) by {}",
                    strikeNumber, User.STRIKES_FOR_BAN, userId, user.getUsername(), moderator.getUsername());
        }
        user.setLastModerationAt(LocalDateTime.now());
        userRepository.save(user);

        // 3. Notify the struck user (moderator is the actor). Permanent record already exists,
        //    so a notification failure must never lose the strike - hence it runs last.
        String notifText = banned
                ? "You have received strike " + strikeNumber + " of " + User.STRIKES_FOR_BAN
                    + " (" + savedWarning.getWarningDate().format(DATE_FMT) + "): " + safeReason
                    + ". Your account is now banned."
                : "You have received a community warning - strike " + strikeNumber + " of "
                    + User.STRIKES_FOR_BAN + " (" + savedWarning.getWarningDate().format(DATE_FMT) + "): "
                    + safeReason + ".";
        notificationService.createNotification(userId, notifText,
                NotificationType.STRIKE_ISSUED, moderator, null, null, null);

        return savedWarning;
    }

    /**
     * Reverses a previously issued warning. The permanent {@link UserWarning} audit record is
     * NOT deleted (audit trail is immutable); instead the user's active {@code warning_count}
     * is decremented (never below zero) and the moderation standing is updated. If the user was
     * banned solely by strikes and the count drops below the threshold, the ban is lifted and
     * the account is reactivated.
     *
     * @return the user whose warning was reversed
     */
    @Transactional
    public User reverseWarning(Long warningId, Long moderatorId) {
        UserWarning warning = userWarningRepository.findById(warningId)
                .orElseThrow(() -> new ResourceNotFoundException("Warning", warningId));
        User moderator = userRepository.findById(moderatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", moderatorId));
        User user = warning.getUser();

        int newCount = Math.max(0, user.getWarningCount() - 1);
        user.setWarningCount(newCount);

        // If the user was banned but no longer meets the strike threshold, lift the ban.
        if (user.getAccountStatus() == AccountStatus.BANNED && newCount < User.STRIKES_FOR_BAN) {
            user.setAccountStatus(AccountStatus.ACTIVE);
            user.setBannedAt(null);
            user.setBannedByUserId(null);
            user.setBanReason(null);
        }
        user.setLastModerationAction("Warning #" + warningId + " reversed by " + moderator.getUsername());
        user.setLastModerationAt(LocalDateTime.now());
        userRepository.save(user);

        logger.info("Warning {} reversed by moderator {}; user {} active count now {}",
                warningId, moderatorId, user.getUserId(), newCount);
        return user;
    }

    @Transactional(readOnly = true)
    public UserWarning getWarningById(Long warningId) {
        return userWarningRepository.findById(warningId)
                .orElseThrow(() -> new ResourceNotFoundException("Warning", warningId));
    }

    /** Full strike history for a user, newest first (profile + admin per-user view). */
    @Transactional(readOnly = true)
    public List<UserWarning> getWarningsForUser(Long userId) {
        return userWarningRepository.findByUserUserIdOrderByWarningDateDescCreatedAtDesc(userId);
    }

    /** Every strike ever issued, newest first (admin dashboard). */
    @Transactional(readOnly = true)
    public List<UserWarning> getAllWarnings() {
        return userWarningRepository.findAllByOrderByWarningDateDescCreatedAtDesc();
    }

    /** Total number of strikes ever recorded (audit count). */
    @Transactional(readOnly = true)
    public long getTotalWarningCount() {
        return userWarningRepository.count();
    }
}
