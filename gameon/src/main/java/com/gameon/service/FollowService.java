package com.gameon.service;

import com.gameon.exception.BusinessRuleException;
import com.gameon.exception.ResourceNotFoundException;
import com.gameon.model.entity.Follow;
import com.gameon.model.entity.User;
import com.gameon.model.enums.NotificationType;
import com.gameon.repository.FollowRepository;
import com.gameon.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service handling follow/unfollow operations.
 * Part of D400 (View User Profile - follow/unfollow).
 * BR5: A user can follow many other users (no limit).
 */
@Service
public class FollowService {

    private static final Logger logger = LoggerFactory.getLogger(FollowService.class);

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public FollowService(FollowRepository followRepository,
                         UserRepository userRepository,
                         NotificationService notificationService) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /**
     * Toggles follow status. If following, unfollows. If not following, follows.
     * Returns true if now following, false if unfollowed.
     */
    @Transactional
    public boolean toggleFollow(Long followerId, Long followedId) {
        // Prevent self-follow
        if (followerId.equals(followedId)) {
            throw new BusinessRuleException("You cannot follow yourself.");
        }

        if (isFollowing(followerId, followedId)) {
            unfollow(followerId, followedId);
            return false;
        } else {
            follow(followerId, followedId);
            return true;
        }
    }

    /**
     * Follows a user.
     */
    @Transactional
    public void follow(Long followerId, Long followedId) {
        if (followerId.equals(followedId)) {
            throw new BusinessRuleException("You cannot follow yourself.");
        }

        if (isFollowing(followerId, followedId)) {
            return; // Already following, no-op
        }

        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", followerId));
        User followed = userRepository.findById(followedId)
                .orElseThrow(() -> new ResourceNotFoundException("User", followedId));

        Follow follow = new Follow(follower, followed);
        followRepository.save(follow);

        // Notify the followed user
        String notifText = follower.getUsername() + " started following you.";
        notificationService.createNotification(followedId, notifText, NotificationType.FOLLOW_NEW);

        logger.info("User {} followed user {}", follower.getUsername(), followed.getUsername());
    }

    /**
     * Unfollows a user.
     */
    @Transactional
    public void unfollow(Long followerId, Long followedId) {
        if (!isFollowing(followerId, followedId)) {
            return; // Not following, no-op
        }

        followRepository.deleteByFollowerAndFollowed(followerId, followedId);
        logger.info("User {} unfollowed user {}", followerId, followedId);
    }

    /**
     * Checks if a user is following another user.
     */
    @Transactional(readOnly = true)
    public boolean isFollowing(Long followerId, Long followedId) {
        return followRepository.existsByIdFollowerUserIdAndIdFollowedUserId(followerId, followedId);
    }

    /**
     * Gets list of user IDs that the given user is following.
     */
    @Transactional(readOnly = true)
    public List<Long> getFollowingIds(Long userId) {
        return followRepository.findFollowingUserIds(userId);
    }

    /**
     * Gets list of user IDs that follow the given user.
     */
    @Transactional(readOnly = true)
    public List<Long> getFollowerIds(Long userId) {
        return followRepository.findFollowerUserIds(userId);
    }

    /** A Game On friend is a mutual follow. */
    @Transactional(readOnly = true)
    public List<Long> getFriendIds(Long userId) {
        return followRepository.findMutualFollowUserIds(userId);
    }

    /**
     * Gets follower count for a user.
     */
    @Transactional(readOnly = true)
    public long getFollowerCount(Long userId) {
        return followRepository.countByIdFollowedUserId(userId);
    }

    /**
     * Gets following count for a user.
     */
    @Transactional(readOnly = true)
    public long getFollowingCount(Long userId) {
        return followRepository.countByIdFollowerUserId(userId);
    }
}
