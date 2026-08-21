package com.gameon.service;

import com.gameon.model.entity.Like;
import com.gameon.model.entity.LikeId;
import com.gameon.model.entity.Post;
import com.gameon.model.entity.User;
import com.gameon.exception.ResourceNotFoundException;
import com.gameon.repository.LikeRepository;
import com.gameon.repository.PostRepository;
import com.gameon.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service handling like/unlike operations on posts.
 * Part of B300 (Browse Posts - like interactions).
 */
@Service
public class LikeService {

    private static final Logger logger = LoggerFactory.getLogger(LikeService.class);

    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public LikeService(LikeRepository likeRepository,
                       PostRepository postRepository,
                       UserRepository userRepository) {
        this.likeRepository = likeRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    /**
     * Toggles like on a post. If already liked, unlikes. If not liked, likes.
     * Returns true if post is now liked, false if unliked.
     */
    @Transactional
    public boolean toggleLike(Long userId, Long postId) {
        Post post = postRepository.findByPostIdAndRemovedAtIsNull(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));
        if (likeRepository.existsByIdUserIdAndIdPostId(userId, postId)) {
            likeRepository.deleteByUserIdAndPostId(userId, postId);
            logger.debug("User {} unliked post {}", userId, postId);
            return false;
        } else {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", userId));
            Like like = new Like(user, post);
            likeRepository.save(like);
            logger.debug("User {} liked post {}", userId, postId);
            return true;
        }
    }

    /**
     * Checks if a user has liked a post.
     */
    @Transactional(readOnly = true)
    public boolean isLikedByUser(Long userId, Long postId) {
        return likeRepository.existsByIdUserIdAndIdPostId(userId, postId);
    }

    /**
     * Gets the total like count for a post.
     */
    @Transactional(readOnly = true)
    public long getLikeCount(Long postId) {
        return likeRepository.countByIdPostId(postId);
    }
}
