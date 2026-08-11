package com.gameon.controller;

import com.gameon.model.dto.PostFeedDto;
import com.gameon.model.entity.Post;
import com.gameon.model.enums.PrivacySetting;
import com.gameon.security.CustomUserDetails;
import com.gameon.service.CommentService;
import com.gameon.service.LikeService;
import com.gameon.service.PostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for B100 (Create Posts), B200 (Manage Posts), B300 (Browse Posts).
 * Handles the Social tab with feed, post creation, editing, deleting, liking, and commenting.
 */
@Controller
public class SocialController {

    private final PostService postService;
    private final LikeService likeService;
    private final CommentService commentService;

    public SocialController(PostService postService,
                            LikeService likeService,
                            CommentService commentService) {
        this.postService = postService;
        this.likeService = likeService;
        this.commentService = commentService;
    }

    // ===== B300: Social Feed =====

    @GetMapping("/social")
    public String feed(@AuthenticationPrincipal CustomUserDetails currentUser,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "ALL") String filter,
                       Model model) {
        Page<PostFeedDto> posts = postService.getFilteredFeed(currentUser.getUserId(), filter, PageRequest.of(page, 10));
        model.addAttribute("posts", posts);
        model.addAttribute("currentUserId", currentUser.getUserId());
        model.addAttribute("currentFilter", filter);
        return "social/feed";
    }

    // ===== B100: Create Post =====

    @GetMapping("/social/create")
    public String showCreatePost(Model model) {
        model.addAttribute("privacySettings", new PrivacySetting[]{PrivacySetting.PUBLIC, PrivacySetting.FOLLOWERS});
        return "social/create";
    }

    @PostMapping("/social/create")
    public String createPost(@AuthenticationPrincipal CustomUserDetails currentUser,
                             @RequestParam String content,
                             @RequestParam String privacySetting,
                             RedirectAttributes redirectAttributes) {
        try {
            PrivacySetting privacy = PrivacySetting.valueOf(privacySetting.toUpperCase());
            postService.createPost(currentUser.getUserId(), content, privacy);
            redirectAttributes.addFlashAttribute("success", "Post created!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/social";
    }

    // ===== B200: Edit Post =====

    @GetMapping("/social/edit/{postId}")
    public String showEditPost(@PathVariable Long postId,
                               @AuthenticationPrincipal CustomUserDetails currentUser,
                               Model model) {
        Post post = postService.getPostById(postId);
        if (!post.getUser().getUserId().equals(currentUser.getUserId())) {
            return "redirect:/social";
        }
        model.addAttribute("post", post);
        model.addAttribute("privacySettings", new PrivacySetting[]{PrivacySetting.PUBLIC, PrivacySetting.FOLLOWERS});
        return "social/edit";
    }

    @PostMapping("/social/edit/{postId}")
    public String editPost(@PathVariable Long postId,
                           @AuthenticationPrincipal CustomUserDetails currentUser,
                           @RequestParam String content,
                           @RequestParam String privacySetting,
                           RedirectAttributes redirectAttributes) {
        try {
            PrivacySetting privacy = PrivacySetting.valueOf(privacySetting.toUpperCase());
            postService.updatePost(postId, currentUser.getUserId(), content, privacy);
            redirectAttributes.addFlashAttribute("success", "Post updated!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/social";
    }

    // ===== B200: Delete Post =====

    @PostMapping("/social/delete/{postId}")
    public String deletePost(@PathVariable Long postId,
                             @AuthenticationPrincipal CustomUserDetails currentUser,
                             RedirectAttributes redirectAttributes) {
        try {
            postService.deletePost(postId, currentUser.getUserId());
            redirectAttributes.addFlashAttribute("success", "Post deleted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/social";
    }

    // ===== B300: Like/Unlike Post =====

    @PostMapping("/social/like/{postId}")
    public String toggleLike(@PathVariable Long postId,
                             @AuthenticationPrincipal CustomUserDetails currentUser,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "ALL") String filter) {
        likeService.toggleLike(currentUser.getUserId(), postId);
        return "redirect:/social?page=" + page + "&filter=" + filter;
    }

    // ===== B300: Add Comment =====

    @PostMapping("/social/comment/{postId}")
    public String addComment(@PathVariable Long postId,
                             @AuthenticationPrincipal CustomUserDetails currentUser,
                             @RequestParam String text,
                             @RequestParam(defaultValue = "0") int page,
                             RedirectAttributes redirectAttributes) {
        try {
            commentService.addComment(currentUser.getUserId(), postId, text);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/social?page=" + page;
    }

    // ===== B300: Delete Comment =====

    @PostMapping("/social/comment/delete/{commentId}")
    public String deleteComment(@PathVariable Long commentId,
                                @AuthenticationPrincipal CustomUserDetails currentUser,
                                RedirectAttributes redirectAttributes) {
        try {
            commentService.deleteComment(commentId, currentUser.getUserId(), currentUser.isModerator());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/social";
    }

    // ===== View Single Post with Comments =====

    @GetMapping("/social/post/{postId}")
    public String viewPost(@PathVariable Long postId,
                           @AuthenticationPrincipal CustomUserDetails currentUser,
                           Model model) {
        Post post = postService.getPostWithUser(postId);
        model.addAttribute("post", post);
        model.addAttribute("comments", commentService.getCommentsForPost(postId));
        model.addAttribute("likeCount", likeService.getLikeCount(postId));
        model.addAttribute("isLiked", likeService.isLikedByUser(currentUser.getUserId(), postId));
        model.addAttribute("currentUserId", currentUser.getUserId());
        return "social/post-detail";
    }
}
