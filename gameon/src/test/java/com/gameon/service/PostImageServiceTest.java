package com.gameon.service;

import com.gameon.exception.BusinessRuleException;
import com.gameon.model.dto.PostImageDto;
import com.gameon.model.entity.Post;
import com.gameon.model.entity.PostImage;
import com.gameon.model.entity.User;
import com.gameon.model.enums.PrivacySetting;
import com.gameon.repository.FollowRepository;
import com.gameon.repository.PostImageRepository;
import com.gameon.repository.PostRepository;
import com.gameon.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Focused unit tests for image handling in {@link PostService}: post validity,
 * the 4-image maximum, display-order assignment/normalisation, and edit add/remove
 * with file cleanup. Uses a mocked {@link ImageStorageService} so no real files are
 * written; storage is simulated by returning a deterministic path.
 */
@ExtendWith(MockitoExtension.class)
class PostImageServiceTest {

    @Mock PostRepository postRepository;
    @Mock UserRepository userRepository;
    @Mock FollowRepository followRepository;
    @Mock PostImageRepository postImageRepository;
    @Mock ImageStorageService imageStorageService;
    @InjectMocks PostService service;

    private User author;

    @BeforeEach
    void setUp() {
        author = new User();
        author.setUserId(1L);
        author.setUsername("Author");
    }

    private MultipartFile img(String name) {
        return new MockMultipartFile("images", name, "image/png", new byte[]{1, 2, 3});
    }

    private List<MultipartFile> images(int n) {
        List<MultipartFile> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(img("img" + i + ".png"));
        }
        return list;
    }

    private void stubUserAndSave() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ===== Creation =====

    @Test
    void createsTextOnlyPost() {
        stubUserAndSave();
        Post post = service.createPost(1L, "Hello world", PrivacySetting.PUBLIC, List.of());

        assertThat(post.getContent()).isEqualTo("Hello world");
        assertThat(post.getImages()).isEmpty();
        verifyNoInteractions(imageStorageService);
    }

    @Test
    void createsImageOnlyPostWithEmptyContent() {
        stubUserAndSave();
        when(imageStorageService.store(any())).thenReturn("/uploads/posts/a.png");

        Post post = service.createPost(1L, "   ", PrivacySetting.PUBLIC, images(1));

        assertThat(post.getContent()).isEmpty();
        assertThat(post.getImages()).hasSize(1);
        assertThat(post.getImages().get(0).getDisplayOrder()).isEqualTo(1);
    }

    @Test
    void createsTextPlusImagesPost() {
        stubUserAndSave();
        when(imageStorageService.store(any())).thenReturn("/uploads/posts/x.png");

        Post post = service.createPost(1L, "caption", PrivacySetting.PUBLIC, images(2));

        assertThat(post.getContent()).isEqualTo("caption");
        assertThat(post.getImages()).hasSize(2);
    }

    @Test
    void acceptsUpToFourImagesWithSequentialOrder() {
        stubUserAndSave();
        when(imageStorageService.store(any()))
                .thenReturn("/uploads/posts/1.png", "/uploads/posts/2.png",
                            "/uploads/posts/3.png", "/uploads/posts/4.png");

        Post post = service.createPost(1L, "", PrivacySetting.PUBLIC, images(4));

        assertThat(post.getImages()).hasSize(4);
        assertThat(post.getImages()).extracting(PostImage::getDisplayOrder)
                .containsExactly(1, 2, 3, 4);
        assertThat(post.getImages()).extracting(PostImage::getImagePath)
                .containsExactly("/uploads/posts/1.png", "/uploads/posts/2.png",
                                 "/uploads/posts/3.png", "/uploads/posts/4.png");
    }

    @Test
    void rejectsFifthImage() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));

        assertThatThrownBy(() -> service.createPost(1L, "hi", PrivacySetting.PUBLIC, images(5)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("at most 4");
        verify(postRepository, never()).save(any());
    }

    @Test
    void rejectsBlankPostWithNoImages() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));

        assertThatThrownBy(() -> service.createPost(1L, "  ", PrivacySetting.PUBLIC, List.of()))
                .isInstanceOf(BusinessRuleException.class);
        verify(postRepository, never()).save(any());
    }

    @Test
    void rollsBackStoredFilesWhenAStoreFailsMidway() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(imageStorageService.store(any()))
                .thenReturn("/uploads/posts/ok.png")
                .thenThrow(new BusinessRuleException("bad image"));

        assertThatThrownBy(() -> service.createPost(1L, "x", PrivacySetting.PUBLIC, images(2)))
                .isInstanceOf(BusinessRuleException.class);
        // The first successfully-stored file must be cleaned up.
        verify(imageStorageService).delete("/uploads/posts/ok.png");
        verify(postRepository, never()).save(any());
    }

    // ===== Editing =====

    private Post existingPostWithImages(int count) {
        Post post = new Post(author, "original", PrivacySetting.PUBLIC);
        post.setPostId(50L);
        for (int i = 0; i < count; i++) {
            post.addImage(new PostImage(post, "/uploads/posts/existing" + (i + 1) + ".png", i + 1));
        }
        return post;
    }

    @Test
    void editRemovesOneImageDeletesFileAndNormalisesOrder() {
        Post post = existingPostWithImages(3);
        when(postRepository.findByPostIdAndRemovedAtIsNull(50L)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        Post updated = service.updatePost(50L, 1L, "original", PrivacySetting.PUBLIC,
                List.of("/uploads/posts/existing2.png"), List.of());

        assertThat(updated.getImages()).extracting(PostImage::getImagePath)
                .containsExactly("/uploads/posts/existing1.png", "/uploads/posts/existing3.png");
        // Order normalised to a clean 1..N.
        assertThat(updated.getImages()).extracting(PostImage::getDisplayOrder)
                .containsExactly(1, 2);
        // The removed image's file is deleted after a successful persist.
        verify(imageStorageService).delete("/uploads/posts/existing2.png");
    }

    @Test
    void editAddsNewImagesAppendedAfterSurvivors() {
        Post post = existingPostWithImages(2);
        when(postRepository.findByPostIdAndRemovedAtIsNull(50L)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));
        when(imageStorageService.store(any())).thenReturn("/uploads/posts/new.png");

        Post updated = service.updatePost(50L, 1L, "original", PrivacySetting.PUBLIC,
                List.of(), images(1));

        assertThat(updated.getImages()).extracting(PostImage::getImagePath)
                .containsExactly("/uploads/posts/existing1.png", "/uploads/posts/existing2.png",
                                 "/uploads/posts/new.png");
        assertThat(updated.getImages()).extracting(PostImage::getDisplayOrder)
                .containsExactly(1, 2, 3);
    }

    @Test
    void editKeepsUnremovedImages() {
        Post post = existingPostWithImages(3);
        when(postRepository.findByPostIdAndRemovedAtIsNull(50L)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        Post updated = service.updatePost(50L, 1L, "changed text", PrivacySetting.PUBLIC,
                List.of(), List.of());

        assertThat(updated.getImages()).hasSize(3);
        assertThat(updated.getContent()).isEqualTo("changed text");
        verify(imageStorageService, never()).delete(anyString());
    }

    @Test
    void editCannotExceedFourTotalImages() {
        Post post = existingPostWithImages(3);
        when(postRepository.findByPostIdAndRemovedAtIsNull(50L)).thenReturn(Optional.of(post));

        // 3 existing + 2 new = 5 > 4  -> rejected before any store/persist.
        assertThatThrownBy(() -> service.updatePost(50L, 1L, "x", PrivacySetting.PUBLIC,
                List.of(), images(2)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("at most 4");
        verify(postRepository, never()).save(any());
        verify(imageStorageService, never()).store(any());
    }

    @Test
    void editRemovingAllImagesFromTextlessPostIsRejected() {
        Post post = existingPostWithImages(2);
        post.setContent("");
        when(postRepository.findByPostIdAndRemovedAtIsNull(50L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> service.updatePost(50L, 1L, "", PrivacySetting.PUBLIC,
                List.of("/uploads/posts/existing1.png", "/uploads/posts/existing2.png"), List.of()))
                .isInstanceOf(BusinessRuleException.class);
        verify(postRepository, never()).save(any());
    }

    // ===== Batch image loading for post-entity pages (e.g. profile posts) =====

    @Test
    void getImagePathsForPostsGroupsByPostPreservingDisplayOrder() {
        // Repository returns rows already ordered by post_id, display_order (as the query guarantees).
        when(postImageRepository.findImageDtosForPosts(List.of(54L, 55L, 56L)))
                .thenReturn(List.of(
                        new PostImageDto(54L, "/uploads/posts/a.jpg", 1),
                        new PostImageDto(54L, "/uploads/posts/b.jpg", 2),
                        new PostImageDto(54L, "/uploads/posts/c.jpg", 3),
                        new PostImageDto(55L, "/uploads/posts/d.png", 1)
                        // 56L has no images
                ));

        Map<Long, List<String>> byPost = service.getImagePathsForPosts(List.of(54L, 55L, 56L));

        assertThat(byPost).containsOnlyKeys(54L, 55L);
        assertThat(byPost.get(54L))
                .containsExactly("/uploads/posts/a.jpg", "/uploads/posts/b.jpg", "/uploads/posts/c.jpg");
        assertThat(byPost.get(55L)).containsExactly("/uploads/posts/d.png");
        assertThat(byPost).doesNotContainKey(56L);
    }

    @Test
    void getImagePathsForPostsUsesSingleBatchQueryNotPerPost() {
        when(postImageRepository.findImageDtosForPosts(any()))
                .thenReturn(List.of(new PostImageDto(1L, "/uploads/posts/x.webp", 1)));

        service.getImagePathsForPosts(List.of(1L, 2L, 3L, 4L));

        // Exactly one batch call for the whole page (no N+1 per-post lookups).
        verify(postImageRepository, times(1)).findImageDtosForPosts(List.of(1L, 2L, 3L, 4L));
        verify(postImageRepository, never()).findByPostPostIdOrderByDisplayOrderAsc(anyLong());
    }

    @Test
    void getImagePathsForPostsReturnsEmptyForNoPosts() {
        assertThat(service.getImagePathsForPosts(List.of())).isEmpty();
        assertThat(service.getImagePathsForPosts(null)).isEmpty();
        verify(postImageRepository, never()).findImageDtosForPosts(any());
    }
}
