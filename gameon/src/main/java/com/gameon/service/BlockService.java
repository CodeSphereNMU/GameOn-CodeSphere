package com.gameon.service;

import com.gameon.exception.BusinessRuleException;
import com.gameon.exception.ResourceNotFoundException;
import com.gameon.model.entity.Block;
import com.gameon.model.entity.User;
import com.gameon.repository.BlockRepository;
import com.gameon.repository.FollowRepository;
import com.gameon.repository.InvitationRepository;
import com.gameon.repository.JoinRequestRepository;
import com.gameon.repository.NotificationRepository;
import com.gameon.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Central service for the mutual (bidirectional) blocking feature.
 *
 * <p>Blocking is stored directionally (blocker -&gt; blocked) but is enforced as a
 * <b>mutual</b> visibility and interaction restriction: if either user has blocked the
 * other ({@code BLOCK(A,B) OR BLOCK(B,A)}), both users are prevented from seeing or
 * interacting with each other.</p>
 *
 * <p>All visibility and interaction checks across the app go through
 * {@link #isBlockedEitherWay(Long, Long)}. Feeds/listings/search use
 * {@link #getBlockedUserIds(Long)} to filter efficiently at the repository level.</p>
 *
 * <p>Historical data (match history, reports, results, audit records) is never deleted by
 * blocking; only future visibility and interaction are prevented, plus pending items
 * (follows, invitations, join requests, notifications) between the two users are cleared.</p>
 */
@Service
public class BlockService {

    private static final Logger logger = LoggerFactory.getLogger(BlockService.class);

    private final BlockRepository blockRepository;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final InvitationRepository invitationRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final NotificationRepository notificationRepository;

    public BlockService(BlockRepository blockRepository,
                        UserRepository userRepository,
                        FollowRepository followRepository,
                        InvitationRepository invitationRepository,
                        JoinRequestRepository joinRequestRepository,
                        NotificationRepository notificationRepository) {
        this.blockRepository = blockRepository;
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.invitationRepository = invitationRepository;
        this.joinRequestRepository = joinRequestRepository;
        this.notificationRepository = notificationRepository;
    }

    // ===== 10. Central enforcement method =====

    /**
     * The single source of truth for blocking. Returns {@code true} if user A blocked user B
     * OR user B blocked user A. All visibility and interaction checks must use this method.
     *
     * <p>Reusable by any current or future feature (feed, listings, profiles, search,
     * following, invitations, join requests, notifications, and future messaging/chat).</p>
     */
    @Transactional(readOnly = true)
    public boolean isBlockedEitherWay(Long userId1, Long userId2) {
        if (userId1 == null || userId2 == null || userId1.equals(userId2)) {
            return false;
        }
        return blockRepository.existsBlockEitherWay(userId1, userId2);
    }

    /** Directional check: has {@code blockerId} explicitly blocked {@code blockedId}? */
    @Transactional(readOnly = true)
    public boolean hasBlocked(Long blockerId, Long blockedId) {
        if (blockerId == null || blockedId == null) {
            return false;
        }
        return blockRepository.existsByIdBlockerUserIdAndIdBlockedUserId(blockerId, blockedId);
    }

    /**
     * The full "block wall" for a user: every user id involved in a block with this user in
     * either direction. Feeds/listings/search pass this to repository queries to exclude
     * blocked users at the database level (no load-all-then-filter-in-Java).
     *
     * <p>The returned list is never {@code null}; callers should guard {@code NOT IN} clauses
     * against an empty list (JPQL does not handle empty {@code IN ()} well), following the
     * existing pattern used for follow-id lists.</p>
     */
    @Transactional(readOnly = true)
    public List<Long> getBlockedUserIds(Long userId) {
        if (userId == null) {
            return List.of();
        }
        // Merge both directions in-service so we never depend on JPQL UNION dialect quirks.
        Set<Long> ids = new LinkedHashSet<>();
        ids.addAll(blockRepository.findBlockedUserIds(userId));
        ids.addAll(blockRepository.findBlockerUserIds(userId));
        return List.copyOf(ids);
    }

    // ===== Block / Unblock actions (5,6,7,8: side effects) =====

    /**
     * Blocks {@code blockedId} on behalf of {@code blockerId}, applying all mutual
     * side effects required by the mutual-blocking rules:
     * <ul>
     *   <li>Removes any existing follow relationship in <b>both</b> directions (5).</li>
     *   <li>Expires any pending invitations between the two users in both directions (6).</li>
     *   <li>Rejects/expires any pending join requests between the two in both directions (7).</li>
     *   <li>Removes pending (unread) notifications between the two users (8).</li>
     * </ul>
     *
     * <p>Historical data is preserved. Idempotent: re-blocking an already-blocked user is a no-op
     * for the block row but the side effects are safely re-applied.</p>
     */
    @Transactional
    public void block(Long blockerId, Long blockedId) {
        if (blockerId == null || blockedId == null) {
            throw new BusinessRuleException("A user id is required to block.");
        }
        if (blockerId.equals(blockedId)) {
            throw new BusinessRuleException("You cannot block yourself.");
        }

        User blocker = userRepository.findById(blockerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", blockerId));
        User blocked = userRepository.findById(blockedId)
                .orElseThrow(() -> new ResourceNotFoundException("User", blockedId));

        if (!blockRepository.existsByIdBlockerUserIdAndIdBlockedUserId(blockerId, blockedId)) {
            blockRepository.save(new Block(blocker, blocked));
            logger.info("User {} blocked user {}", blocker.getUsername(), blocked.getUsername());
        }

        applyMutualSideEffects(blockerId, blockedId);
    }

    /**
     * Removes a directional block ({@code blockerId} -&gt; {@code blockedId}). The reverse block,
     * if any, is left intact so the relationship can still be mutually restricted.
     */
    @Transactional
    public void unblock(Long blockerId, Long blockedId) {
        if (blockerId == null || blockedId == null) {
            return;
        }
        if (blockRepository.existsByIdBlockerUserIdAndIdBlockedUserId(blockerId, blockedId)) {
            blockRepository.deleteByBlockerAndBlocked(blockerId, blockedId);
            logger.info("User {} unblocked user {}", blockerId, blockedId);
        }
    }

    /** Convenience toggle used by the block button. Returns true if now blocked, false if unblocked. */
    @Transactional
    public boolean toggleBlock(Long blockerId, Long blockedId) {
        if (blockerId != null && blockerId.equals(blockedId)) {
            throw new BusinessRuleException("You cannot block yourself.");
        }
        if (hasBlocked(blockerId, blockedId)) {
            unblock(blockerId, blockedId);
            return false;
        }
        block(blockerId, blockedId);
        return true;
    }

    /**
     * Applies the mutual clean-up of relationships and pending items between two users.
     * Extracted so it runs both when a fresh block is created and (idempotently) on re-block.
     */
    private void applyMutualSideEffects(Long a, Long b) {
        // 5. Remove any existing follow relationship in both directions; prevent future follows
        //    is enforced in FollowService via isBlockedEitherWay.
        followRepository.deleteByFollowerAndFollowed(a, b);
        followRepository.deleteByFollowerAndFollowed(b, a);

        // 6 & 7. Expire pending invitations and reject/expire pending join requests between them,
        //        for listings created by either user (both directions).
        invitationRepository.expirePendingBetweenUsers(a, b);
        invitationRepository.expirePendingBetweenUsers(b, a);
        joinRequestRepository.expirePendingBetweenUsers(a, b);
        joinRequestRepository.expirePendingBetweenUsers(b, a);

        // 8. Remove pending notifications between the two users (either as recipient/actor).
        notificationRepository.deleteBetweenUsers(a, b);
    }
}
