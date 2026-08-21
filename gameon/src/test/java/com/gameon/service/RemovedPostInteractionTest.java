package com.gameon.service;

import com.gameon.exception.ResourceNotFoundException;
import com.gameon.repository.CommentRepository;
import com.gameon.repository.LikeRepository;
import com.gameon.repository.PostRepository;
import com.gameon.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RemovedPostInteractionTest {

    @Mock PostRepository postRepository;
    @Mock UserRepository userRepository;
    @Mock LikeRepository likeRepository;
    @Mock CommentRepository commentRepository;

    @Test
    void removedPostCannotBeLikedOrUnliked() {
        when(postRepository.findByPostIdAndRemovedAtIsNull(20L)).thenReturn(Optional.empty());
        LikeService service = new LikeService(likeRepository, postRepository, userRepository);

        assertThatThrownBy(() -> service.toggleLike(1L, 20L))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(likeRepository, userRepository);
    }

    @Test
    void removedPostCannotReceiveNewComments() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mock(com.gameon.model.entity.User.class)));
        when(postRepository.findByPostIdAndRemovedAtIsNull(20L)).thenReturn(Optional.empty());
        CommentService service = new CommentService(commentRepository, postRepository, userRepository);

        assertThatThrownBy(() -> service.addComment(1L, 20L, "Comment"))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(commentRepository);
    }
}
