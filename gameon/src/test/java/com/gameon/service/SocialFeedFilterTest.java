package com.gameon.service;

import com.gameon.model.dto.PostFeedDto;
import com.gameon.model.enums.PrivacySetting;
import com.gameon.repository.FollowRepository;
import com.gameon.repository.LikeRepository;
import com.gameon.repository.PostImageRepository;
import com.gameon.repository.PostRepository;
import com.gameon.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Focused tests for the Social feed filters after the PUBLIC/FOLLOWERS -> FOLLOWING change (B300).
 * Verifies the FOLLOWING filter routing, its privacy guarantees (only followed authors), the
 * block-filtered variant, and that retired filter values fall back safely to ALL.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SocialFeedFilterTest {

    @Mock PostRepository postRepository;
    @Mock UserRepository userRepository;
    @Mock FollowRepository followRepository;
    @Mock PostImageRepository postImageRepository;
    @Mock ImageStorageService imageStorageService;
    @Mock BlockService blockService;
    @Mock LikeRepository likeRepository;

    private PostService service() {
        return new PostService(postRepository, userRepository, followRepository,
                postImageRepository, imageStorageService, blockService, likeRepository);
    }

    private static final Long VIEWER = 1L;
    private static final Pageable PAGE = PageRequest.of(0, 10);

    private static PostFeedDto dto(long postId, long authorId, PrivacySetting privacy) {
        return new PostFeedDto(postId, "content", privacy, LocalDateTime.now(),
                authorId, "author" + authorId, 0L, 0L);
    }

    @Test
    void followingFilterReturnsPublicAndFollowersPostsFromFollowedAuthorsOnly() {
        List<Long> followed = List.of(2L, 3L);
        when(blockService.getBlockedUserIds(VIEWER)).thenReturn(List.of());
        when(followRepository.findFollowingUserIds(VIEWER)).thenReturn(followed);
        // Query returns a PUBLIC and a FOLLOWERS post, both from followed authors.
        Page<PostFeedDto> expected = new PageImpl<>(List.of(
                dto(10L, 2L, PrivacySetting.PUBLIC),
                dto(11L, 3L, PrivacySetting.FOLLOWERS)));
        when(postRepository.findFollowingPostDtos(followed, PAGE)).thenReturn(expected);

        Page<PostFeedDto> result = service().getFilteredFeed(VIEWER, "FOLLOWING", PAGE);

        // Both privacy levels are included, and the followed-ids list is passed verbatim so no
        // non-followed author can ever be surfaced by this filter.
        assertThat(result.getContent()).extracting(PostFeedDto::postId).containsExactly(10L, 11L);
        verify(postRepository).findFollowingPostDtos(followed, PAGE);
        // The retired PUBLIC/FOLLOWERS-specific routing and the ALL routing are not used here.
        verify(postRepository, never()).findAllVisiblePostDtos(any(), any(), any());
        verify(postRepository, never()).findMyPostDtos(any(), any());
    }

    @Test
    void followingFilterReturnsEmptyWhenUserFollowsNobody() {
        when(blockService.getBlockedUserIds(VIEWER)).thenReturn(List.of());
        when(followRepository.findFollowingUserIds(VIEWER)).thenReturn(List.of());

        Page<PostFeedDto> result = service().getFilteredFeed(VIEWER, "FOLLOWING", PAGE);

        assertThat(result.getContent()).isEmpty();
        // No following query is executed when there are no followed users.
        verify(postRepository, never()).findFollowingPostDtos(any(), any());
        verify(postRepository, never()).findFollowingPostDtosExcludingBlocked(any(), any(), any());
    }

    @Test
    void followingFilterUsesBlockExcludingVariantWhenViewerHasBlocks() {
        List<Long> followed = List.of(2L, 3L);
        List<Long> blocked = List.of(3L);
        when(blockService.getBlockedUserIds(VIEWER)).thenReturn(blocked);
        when(followRepository.findFollowingUserIds(VIEWER)).thenReturn(followed);
        when(postRepository.findFollowingPostDtosExcludingBlocked(followed, blocked, PAGE))
                .thenReturn(new PageImpl<>(List.of(dto(10L, 2L, PrivacySetting.FOLLOWERS))));

        Page<PostFeedDto> result = service().getFilteredFeed(VIEWER, "FOLLOWING", PAGE);

        assertThat(result.getContent()).extracting(PostFeedDto::postId).containsExactly(10L);
        verify(postRepository).findFollowingPostDtosExcludingBlocked(followed, blocked, PAGE);
        verify(postRepository, never()).findFollowingPostDtos(any(), any());
    }

    @Test
    void retiredPublicFilterFallsBackToAllVisibleFeed() {
        List<Long> followed = List.of(2L);
        when(blockService.getBlockedUserIds(VIEWER)).thenReturn(List.of());
        when(followRepository.findFollowingUserIds(VIEWER)).thenReturn(followed);
        when(postRepository.findAllVisiblePostDtos(eq(VIEWER), eq(followed), eq(PAGE)))
                .thenReturn(new PageImpl<>(List.of(dto(10L, 2L, PrivacySetting.PUBLIC))));

        Page<PostFeedDto> result = service().getFilteredFeed(VIEWER, "PUBLIC", PAGE);

        // Retired value routes through ALL (privacy fully enforced), never a removed query path.
        assertThat(result.getContent()).extracting(PostFeedDto::postId).containsExactly(10L);
        verify(postRepository).findAllVisiblePostDtos(VIEWER, followed, PAGE);
    }

    @Test
    void retiredFollowersFilterFallsBackToAllVisibleFeed() {
        List<Long> followed = List.of(2L);
        when(blockService.getBlockedUserIds(VIEWER)).thenReturn(List.of());
        when(followRepository.findFollowingUserIds(VIEWER)).thenReturn(followed);
        when(postRepository.findAllVisiblePostDtos(eq(VIEWER), eq(followed), eq(PAGE)))
                .thenReturn(new PageImpl<>(List.of(dto(10L, 2L, PrivacySetting.PUBLIC))));

        Page<PostFeedDto> result = service().getFilteredFeed(VIEWER, "FOLLOWERS", PAGE);

        assertThat(result.getContent()).extracting(PostFeedDto::postId).containsExactly(10L);
        verify(postRepository).findAllVisiblePostDtos(VIEWER, followed, PAGE);
        // The FOLLOWING routing must not be triggered by the retired FOLLOWERS value.
        verify(postRepository, never()).findFollowingPostDtos(any(), any());
    }
}
