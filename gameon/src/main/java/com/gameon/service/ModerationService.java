package com.gameon.service;

import com.gameon.exception.ResourceNotFoundException;
import com.gameon.model.entity.ModerationAction;
import com.gameon.model.entity.Report;
import com.gameon.model.entity.User;
import com.gameon.model.enums.ContentType;
import com.gameon.model.enums.ModerationActionType;
import com.gameon.repository.ModerationActionRepository;
import com.gameon.repository.ReportRepository;
import com.gameon.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Central audit service for moderator actions. Every moderator/administrator action is recorded
 * permanently as a {@link ModerationAction} row so the full moderation history is available for
 * dashboards, readable views and demonstrations. Records are never deleted.
 */
@Service
public class ModerationService {

    private static final Logger logger = LoggerFactory.getLogger(ModerationService.class);

    private final ModerationActionRepository moderationActionRepository;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;

    public ModerationService(ModerationActionRepository moderationActionRepository,
                             UserRepository userRepository,
                             ReportRepository reportRepository) {
        this.moderationActionRepository = moderationActionRepository;
        this.userRepository = userRepository;
        this.reportRepository = reportRepository;
    }

    /**
     * Records a moderator action in the audit trail.
     *
     * @param moderatorId  the acting moderator (required)
     * @param targetUserId the user affected, if any (nullable)
     * @param reportId     the report that prompted the action, if any (nullable)
     * @param type         the action type (required)
     * @param reason       human-readable reason/context (nullable)
     * @param contentType  the content type involved, if any (nullable)
     * @param contentId    the content id involved, if any (nullable)
     */
    @Transactional
    public ModerationAction logAction(Long moderatorId, Long targetUserId, Long reportId,
                                      ModerationActionType type, String reason,
                                      ContentType contentType, Long contentId) {
        User moderator = userRepository.findById(moderatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", moderatorId));
        User target = targetUserId != null ? userRepository.findById(targetUserId).orElse(null) : null;
        Report report = reportId != null ? reportRepository.findById(reportId).orElse(null) : null;

        ModerationAction action = new ModerationAction(moderator, target, report, type, reason, contentType, contentId);
        ModerationAction saved = moderationActionRepository.save(action);
        logger.info("Moderation action logged: {} by moderator {} (target={}, report={})",
                type, moderatorId, targetUserId, reportId);
        return saved;
    }

    /** All moderation actions, newest first (full audit history). */
    @Transactional(readOnly = true)
    public List<ModerationAction> getAllActions() {
        return moderationActionRepository.findAllByOrderByCreatedAtDesc();
    }

    /** The most recent moderation actions, newest first (dashboard). */
    @Transactional(readOnly = true)
    public List<ModerationAction> getRecentActions(int limit) {
        return moderationActionRepository.findByOrderByCreatedAtDesc(PageRequest.of(0, limit));
    }

    /** Moderation actions targeting a specific user, newest first. */
    @Transactional(readOnly = true)
    public List<ModerationAction> getActionsForUser(Long userId) {
        return moderationActionRepository.findByTargetUserUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public long getTotalActionCount() {
        return moderationActionRepository.count();
    }
}
