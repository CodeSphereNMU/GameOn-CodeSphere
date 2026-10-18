package com.gameon.controller;

import com.gameon.model.entity.User;
import com.gameon.model.enums.UserRole;
import com.gameon.security.CustomUserDetails;
import com.gameon.service.BlockService;
import com.gameon.service.FollowService;
import com.gameon.service.FriendsLeaderboardService;
import com.gameon.service.MatchResultService;
import com.gameon.service.PostService;
import com.gameon.service.ProfileService;
import com.gameon.service.SportService;
import com.gameon.service.UserService;
import com.gameon.service.WarningService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Server-side enforcement of mutual blocking on the profile view ({@code /profile/{userId}}).
 * A blocked relationship (in either direction) must prevent profile access and must not expose
 * any personal or sport-profile data - even when the URL is entered directly. Normal profiles
 * remain accessible, and unblocking restores access.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProfileBlockVisibilityTest {

    @Mock ProfileService profileService;
    @Mock UserService userService;
    @Mock SportService sportService;
    @Mock FollowService followService;
    @Mock PostService postService;
    @Mock MatchResultService matchResultService;
    @Mock FriendsLeaderboardService friendsLeaderboardService;
    @Mock BlockService blockService;
    @Mock WarningService warningService;
    @InjectMocks ProfileController controller;

    private CustomUserDetails alice; // id 1 (the reporter/blocker)
    private static final long BOB_ID = 2L; // the reported/blocked user

    @BeforeEach
    void setUp() {
        User a = new User("Alice", "password", UserRole.USER);
        a.setUserId(1L);
        alice = new CustomUserDetails(a);
    }

    @Test
    void reporterCannotViewBlockedUsersProfile() {
        // Alice blocked Bob (or vice-versa): isBlockedEitherWay is true.
        when(blockService.isBlockedEitherWay(1L, BOB_ID)).thenReturn(true);
        Model model = new ExtendedModelMap();

        String view = controller.viewProfile(BOB_ID, alice, model);

        assertThat(view).isEqualTo("profile/unavailable");
        // No profile data must be loaded or exposed.
        verify(profileService, never()).getProfile(anyLong());
        verify(profileService, never()).getUserSports(anyLong());
        assertThat(model.getAttribute("user")).isNull();
        assertThat(model.getAttribute("sports")).isNull();
    }

    @Test
    void blockedUserCannotViewReporterProfileDirectUrlCannotBypass() {
        // Reverse direction: Bob viewing Alice. The check is symmetric, so still blocked.
        User b = new User("Bob", "password", UserRole.USER);
        b.setUserId(BOB_ID);
        CustomUserDetails bob = new CustomUserDetails(b);
        when(blockService.isBlockedEitherWay(BOB_ID, 1L)).thenReturn(true);
        Model model = new ExtendedModelMap();

        // Directly requesting /profile/1 as Bob (simulated by calling the handler).
        String view = controller.viewProfile(1L, bob, model);

        assertThat(view).isEqualTo("profile/unavailable");
        verify(profileService, never()).getProfile(anyLong());
    }

    @Test
    void normalProfileRemainsAccessibleWhenNoBlock() {
        when(blockService.isBlockedEitherWay(1L, BOB_ID)).thenReturn(false);
        User bob = new User("Bob", "password", UserRole.USER);
        bob.setUserId(BOB_ID);
        when(profileService.getProfile(BOB_ID)).thenReturn(bob);
        when(profileService.getUserSports(BOB_ID)).thenReturn(List.of());
        when(profileService.getFollowerCount(BOB_ID)).thenReturn(0L);
        when(profileService.getFollowingCount(BOB_ID)).thenReturn(0L);
        when(followService.isFollowing(1L, BOB_ID)).thenReturn(false);
        when(friendsLeaderboardService.getFriendsLeaderboard(BOB_ID)).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.viewProfile(BOB_ID, alice, model);

        assertThat(view).isEqualTo("profile/view");
        assertThat(model.getAttribute("user")).isSameAs(bob);
        verify(profileService).getProfile(BOB_ID);
    }

    @Test
    void unblockingRestoresProfileAccess() {
        User bob = new User("Bob", "password", UserRole.USER);
        bob.setUserId(BOB_ID);
        when(profileService.getProfile(BOB_ID)).thenReturn(bob);
        when(profileService.getUserSports(BOB_ID)).thenReturn(List.of());
        when(friendsLeaderboardService.getFriendsLeaderboard(BOB_ID)).thenReturn(List.of());

        // First blocked -> unavailable.
        when(blockService.isBlockedEitherWay(1L, BOB_ID)).thenReturn(true);
        assertThat(controller.viewProfile(BOB_ID, alice, new ExtendedModelMap()))
                .isEqualTo("profile/unavailable");

        // After unblock (no block either way) -> accessible again.
        when(blockService.isBlockedEitherWay(1L, BOB_ID)).thenReturn(false);
        assertThat(controller.viewProfile(BOB_ID, alice, new ExtendedModelMap()))
                .isEqualTo("profile/view");
    }

    @Test
    void viewingOwnProfileRedirectsRegardlessOfBlockCheck() {
        String view = controller.viewProfile(1L, alice, new ExtendedModelMap());
        assertThat(view).isEqualTo("redirect:/profile");
        verify(blockService, never()).isBlockedEitherWay(anyLong(), anyLong());
    }
}
