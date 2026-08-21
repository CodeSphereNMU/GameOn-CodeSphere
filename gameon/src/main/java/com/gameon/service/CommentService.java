package com.gameon.service;

import com.gameon.exception.ResourceNotFoundException;
import com.gameon.exception.UnauthorizedAccessException;
import com.gameon.model.entity.Comment;
import com.gameon.model.entity.Post;
import com.gameon.model.entity.User;
import com.gameon.repository.CommentRepository;
import com.gameon.repository.PostRepository;
import com.gameon.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service handling comment operations on posts.
 * Part of B300 (Browse Posts - comment interactions).
 */
@Service
public class CommentService {

    private static final Logger logger = LoggerFactory.getLogger(CommentService.class);

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public CommentService(CommentRepository commentRepository,
                          PostRepository postRepository,
                          UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    /**
     * Adds a comment to a post.
     */
    @Transactional
    public Comment addComment(Long userId, Long postId, String text) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Post post = postRepository.findByPostIdAndRemovedAtIsNull(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));

        Comment comment = new Comment(user, post, text);
        Comment saved = commentRepository.save(comment);
        logger.debug("Comment added: User {} → Post {} (ID: {})", userId, postId, saved.getCommentId());
        return saved;
    }

    /**
     * Deletes a comment. Only the comment owner or a moderator can delete.
     */
    @Transactional
    public void deleteComment(Long commentId, Long userId, boolean isModerator) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));

        if (!comment.getUser().getUserId().equals(userId) && !isModerator) {
            throw new UnauthorizedAccessException("delete", "comment");
        }

        commentRepository.delete(comment);
        logger.info("Comment {} deleted by user {} (moderator: {})", commentId, userId, isModerator);
    }

    /**
     * Gets all comments for a post, ordered by creation date.
     */
    @Transactional(readOnly = true)
    public List<Comment> getCommentsForPost(Long postId) {
        return commentRepository.findByPostIdWithUser(postId);
    }

    /**
     * Gets comment count for a post.
     */
    @Transactional(readOnly = true)
    public long getCommentCount(Long postId) {
        return commentRepository.countByPostPostId(postId);
    }
}
