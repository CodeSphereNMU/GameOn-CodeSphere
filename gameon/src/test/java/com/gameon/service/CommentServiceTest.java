package com.gameon.service;

import com.gameon.model.entity.Comment;
import com.gameon.model.entity.Post;
import com.gameon.model.entity.User;
import com.gameon.model.enums.NotificationType;
import com.gameon.model.enums.PrivacySetting;
import com.gameon.model.enums.UserRole;
import com.gameon.repository.CommentRepository;
import com.gameon.repository.PostRepository;
import com.gameon.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock CommentRepository commentRepository;
    @Mock PostRepository postRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationService notificationService;
    @InjectMocks CommentService service;

    @Test
    void commentOnAnotherUsersPostNotifiesThePostOwner() {
        User author = new User("PostOwner", "password", UserRole.USER);
        author.setUserId(1L);
        User commenter = new User("Commenter", "password", UserRole.USER);
        commenter.setUserId(2L);
        Post post = new Post(author, "Post", PrivacySetting.PUBLIC);
        when(userRepository.findById(2L)).thenReturn(Optional.of(commenter));
        when(postRepository.findByPostIdAndRemovedAtIsNull(10L)).thenReturn(Optional.of(post));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.addComment(2L, 10L, "Nice post");

        verify(notificationService).createNotification(1L, "Commenter commented on your post.",
                NotificationType.POST_COMMENTED, commenter, null, null, null, post);
    }

    @Test
    void commentOnOwnPostDoesNotNotifyTheAuthor() {
        User author = new User("PostOwner", "password", UserRole.USER);
        author.setUserId(1L);
        Post post = new Post(author, "Post", PrivacySetting.PUBLIC);
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(postRepository.findByPostIdAndRemovedAtIsNull(10L)).thenReturn(Optional.of(post));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.addComment(1L, 10L, "My comment");

        verifyNoInteractions(notificationService);
    }
}
