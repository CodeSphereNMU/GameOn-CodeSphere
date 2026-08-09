package com.gameon.controller;

import com.gameon.model.entity.User;
import com.gameon.model.entity.UserSportProfile;
import com.gameon.model.enums.SkillLevel;
import com.gameon.security.CustomUserDetails;
import com.gameon.service.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller for user profile management.
 * D200 (Manage Profile), D300 (Add Sport), D400 (View Profile + Follow/Unfollow).
 */
@Controller
public class ProfileController {

    private final ProfileService profileService;
    private final UserService userService;
    private final SportService sportService;
    private final FollowService followService;
    private final PostService postService;

    public ProfileController(ProfileService profileService,
                             UserService userService,
                             SportService sportService,
                             FollowService followService,
                             PostService postService) {
        this.profileService = profileService;
        this.userService = userService;
        this.sportService = sportService;
        this.followService = followService;
        this.postService = postService;
    }

    // ===== D200: View Own Profile =====

    @GetMapping("/profile")
    public String myProfile(@AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        User user = profileService.getProfile(currentUser.getUserId());
        List<UserSportProfile> sports = profileService.getUserSports(currentUser.getUserId());

        model.addAttribute("user", user);
        model.addAttribute("sports", sports);
        model.addAttribute("followerCount", profileService.getFollowerCount(currentUser.getUserId()));
        model.addAttribute("followingCount", profileService.getFollowingCount(currentUser.getUserId()));
        model.addAttribute("isOwnProfile", true);
        return "profile/view";
    }

    // ===== D200: Edit Profile =====

    @GetMapping("/profile/edit")
    public String editProfile(@AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        User user = userService.getUserById(currentUser.getUserId());
        model.addAttribute("user", user);
        return "profile/edit";
    }

    @PostMapping("/profile/edit")
    public String updateProfile(@AuthenticationPrincipal CustomUserDetails currentUser,
                                @RequestParam String username,
                                RedirectAttributes redirectAttributes) {
        try {
            userService.updateUsername(currentUser.getUserId(), username);
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }

    // ===== D300: Add Sport =====

    @GetMapping("/profile/add-sport")
    public String showAddSport(@AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        model.addAttribute("sports", sportService.getAllSports());
        model.addAttribute("skillLevels", SkillLevel.values());
        model.addAttribute("userSportIds", profileService.getUserSportIds(currentUser.getUserId()));
        return "profile/add-sport";
    }

    @PostMapping("/profile/add-sport")
    public String addSport(@AuthenticationPrincipal CustomUserDetails currentUser,
                           @RequestParam Long sportId,
                           @RequestParam SkillLevel skillLevel,
                           RedirectAttributes redirectAttributes) {
        try {
            profileService.addSportToProfile(currentUser.getUserId(), sportId, skillLevel);
            redirectAttributes.addFlashAttribute("success", "Sport added to your profile!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }

    // ===== D200: Remove Sport =====

    @PostMapping("/profile/remove-sport")
    public String removeSport(@AuthenticationPrincipal CustomUserDetails currentUser,
                              @RequestParam Long sportId,
                              RedirectAttributes redirectAttributes) {
        try {
            profileService.removeSportFromProfile(currentUser.getUserId(), sportId);
            redirectAttributes.addFlashAttribute("success", "Sport removed from your profile");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }

    // ===== D400: View Other User Profile =====

    @GetMapping("/profile/{userId}")
    public String viewProfile(@PathVariable Long userId,
                              @AuthenticationPrincipal CustomUserDetails currentUser,
                              Model model) {
        if (userId.equals(currentUser.getUserId())) {
            return "redirect:/profile";
        }

        User user = profileService.getProfile(userId);
        List<UserSportProfile> sports = profileService.getUserSports(userId);

        model.addAttribute("user", user);
        model.addAttribute("sports", sports);
        model.addAttribute("followerCount", profileService.getFollowerCount(userId));
        model.addAttribute("followingCount", profileService.getFollowingCount(userId));
        model.addAttribute("isOwnProfile", false);
        model.addAttribute("isFollowing", followService.isFollowing(currentUser.getUserId(), userId));
        return "profile/view";
    }

    // ===== D400: Follow/Unfollow Toggle =====

    @PostMapping("/profile/{userId}/follow")
    public String toggleFollow(@PathVariable Long userId,
                               @AuthenticationPrincipal CustomUserDetails currentUser,
                               RedirectAttributes redirectAttributes) {
        try {
            boolean nowFollowing = followService.toggleFollow(currentUser.getUserId(), userId);
            redirectAttributes.addFlashAttribute("success",
                    nowFollowing ? "You are now following this user" : "You unfollowed this user");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile/" + userId;
    }

    // ===== User Search =====

    @GetMapping("/profile/search")
    public String searchUsers(@RequestParam(required = false) String q,
                              @AuthenticationPrincipal CustomUserDetails currentUser,
                              Model model) {
        if (q != null && !q.isBlank()) {
            model.addAttribute("users", userService.searchUsers(q));
            model.addAttribute("query", q);
        }
        return "profile/search";
    }
}
