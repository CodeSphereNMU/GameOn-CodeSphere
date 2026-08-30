package com.gameon.service;

import com.gameon.exception.ResourceNotFoundException;
import com.gameon.model.entity.Post;
import com.gameon.model.entity.Report;
import com.gameon.model.entity.User;
import com.gameon.model.enums.PrivacySetting;
import com.gameon.model.enums.UserRole;
import com.gameon.repository.FollowRepository;
import com.gameon.repository.PostRepository;
import com.gameon.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostSoftRemovalTest {

    @Mock PostRepository postRepository;
    @Mock UserRepository userRepository;
    @Mock FollowRepository followRepository;
    @Mock com.gameon.repository.PostImageRepository postImageRepository;
    @Mock ImageStorageService imageStorageService;
    @InjectMocks PostService service;

    private User author;
    private User moderator;
    private Post post;

    @BeforeEach
    void setUp() {
        author = user(1L, "Author");
        moderator = user(9L, "Moderator");
        post = new Post(author, "Original reported content", PrivacySetting.PUBLIC);
        post.setPostId(20L);
    }

    @Test
    void authorRemovalPreservesPostAndExistingReportReference() {
        Report report = new Report(user(2L, "Reporter"), "Spam");
        report.setReportedPost(post);
        when(postRepository.findByPostIdAndRemovedAtIsNull(20L)).thenReturn(Optional.of(post));

        service.deletePost(20L, 1L);

        assertThat(post.getRemovedAt()).isNotNull();
        assertThat(post.getRemovedBy()).isNull();
        assertThat(report.getReportedPost()).isSameAs(post);
        verify(postRepository).save(post);
        verify(postRepository, never()).delete(any());
    }

    @Test
    void removedPostIsAbsentFromNormalRetrievalAndProfileQueriesUseActiveFilter() {
        when(postRepository.findByPostIdAndRemovedAtIsNull(20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPostById(20L))
                .isInstanceOf(ResourceNotFoundException.class);
        service.getPostsByUser(1L, PageRequest.of(0, 10));
        verify(postRepository).findByUserUserIdAndRemovedAtIsNullOrderByCreatedAtDesc(
                1L, PageRequest.of(0, 10));
    }

    @Test
    void removedPostCannotBeEdited() {
        when(postRepository.findByPostIdAndRemovedAtIsNull(20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updatePost(
                20L, 1L, "Changed", PrivacySetting.PUBLIC))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(postRepository, never()).save(any());
    }

    @Test
    void moderatorRemovalRecordsActingModerator() {
        when(postRepository.findByIdWithUserForModeration(20L)).thenReturn(Optional.of(post));
        when(userRepository.findById(9L)).thenReturn(Optional.of(moderator));

        service.deletePostAsModerator(20L, 9L);

        assertThat(post.getRemovedAt()).isNotNull();
        assertThat(post.getRemovedBy()).isSameAs(moderator);
        verify(postRepository).save(post);
    }

    @Test
    void moderatorReviewDoesNotOverwriteEarlierAuthorRemovalAttribution() {
        LocalDateTime authorRemovalTime = LocalDateTime.now().minusHours(1);
        post.setRemovedAt(authorRemovalTime);
        post.setRemovedBy(null);
        when(postRepository.findByIdWithUserForModeration(20L)).thenReturn(Optional.of(post));

        service.deletePostAsModerator(20L, 9L);

        assertThat(post.getRemovedAt()).isEqualTo(authorRemovalTime);
        assertThat(post.getRemovedBy()).isNull();
        verifyNoInteractions(userRepository);
        verify(postRepository, never()).save(any());
    }

    @Test
    void moderatorCanRetrieveRemovedPostWithOriginalContent() {
        post.setRemovedAt(LocalDateTime.now());
        when(postRepository.findByIdWithUserForModeration(20L)).thenReturn(Optional.of(post));

        Post found = service.getPostForModeration(20L);

        assertThat(found.getContent()).isEqualTo("Original reported content");
    }

    private User user(Long id, String name) {
        User user = new User(name, "password", UserRole.USER);
        user.setUserId(id);
        return user;
    }
}
