package com.gameon.repository;

import com.gameon.model.entity.ModerationAction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link ModerationAction} - the permanent moderation audit trail.
 * Records are never deleted; this repository only reads and appends.
 */
@Repository
public interface ModerationActionRepository extends JpaRepository<ModerationAction, Long> {

    /** All moderation actions, newest first (admin moderation history). */
    List<ModerationAction> findAllByOrderByCreatedAtDesc();

    /** Most recent moderation actions, newest first (dashboard "recent actions"). */
    List<ModerationAction> findByOrderByCreatedAtDesc(Pageable pageable);

    /** Moderation actions targeting a specific user, newest first. */
    List<ModerationAction> findByTargetUserUserIdOrderByCreatedAtDesc(Long targetUserId);
}
