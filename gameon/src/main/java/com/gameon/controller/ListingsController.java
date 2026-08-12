package com.gameon.controller;

import com.gameon.model.entity.GameListing;
import com.gameon.model.entity.SportFormat;
import com.gameon.model.enums.JoinerStatus;
import com.gameon.model.enums.PrivacySetting;
import com.gameon.model.enums.SkillLevel;
import com.gameon.security.CustomUserDetails;
import com.gameon.service.FollowService;
import com.gameon.service.GameJoinerService;
import com.gameon.service.GameListingService;
import com.gameon.service.SportService;
import com.gameon.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for the Listings tab - the main landing page after login.
 * Handles A100 (Create), A200 (Browse), and listing detail views.
 */
@Controller
public class ListingsController {

    private final GameListingService gameListingService;
    private final SportService sportService;
    private final FollowService followService;
    private final UserService userService;
    private final GameJoinerService gameJoinerService;

    public ListingsController(GameListingService gameListingService,
                              SportService sportService,
                              FollowService followService,
                              UserService userService,
                              GameJoinerService gameJoinerService) {
        this.gameListingService = gameListingService;
        this.sportService = sportService;
        this.followService = followService;
        this.userService = userService;
        this.gameJoinerService = gameJoinerService;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/listings";
    }

    // ===== A200: Browse Available Listings (PUBLIC only) =====

    @GetMapping("/listings")
    public String index(@AuthenticationPrincipal CustomUserDetails currentUser,
                        @RequestParam(required = false) String skill,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        Page<GameListing> listings;
        if (skill != null && !skill.isBlank()) {
            SkillLevel skillLevel = SkillLevel.valueOf(skill.toUpperCase());
            listings = gameListingService.browseAvailableListings(
                    currentUser.getUserId(), skillLevel, PageRequest.of(page, 12));
            model.addAttribute("selectedSkill", skill);
        } else {
            listings = gameListingService.browseAvailableListings(
                    currentUser.getUserId(), PageRequest.of(page, 12));
        }

        model.addAttribute("listings", listings);
        model.addAttribute("skillLevels", SkillLevel.values());
        return "listings/index";
    }

    // ===== A100: Create Game Listing (Step 1 - Form) =====

    @GetMapping("/listings/create")
    public String showCreateForm(@AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        List<SportFormat> formats = sportService.getFormatsForUserSports(currentUser.getUserId());
        model.addAttribute("formats", formats);
        model.addAttribute("skillLevels", SkillLevel.values());
        model.addAttribute("privacySettings", PrivacySetting.values());
        return "listings/create";
    }

    // ===== A100: Create Game Listing (Step 2 - Positions, if applicable) =====

    @PostMapping("/listings/create")
    public String processCreate(@AuthenticationPrincipal CustomUserDetails currentUser,
                                @RequestParam Long formatId,
                                @RequestParam String skillLevel,
                                @RequestParam String scheduledDate,
                                @RequestParam String location,
                                @RequestParam String privacySetting,
                                @RequestParam Integer sessionDuration,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        SportFormat format = sportService.getFormatById(formatId);

        // If format has positions, redirect to position selection step
        if (format.getHasPositions()) {
            model.addAttribute("format", format);
            model.addAttribute("positions", sportService.getPositionsForFormat(formatId));
            model.addAttribute("formatId", formatId);
            model.addAttribute("skillLevel", skillLevel);
            model.addAttribute("scheduledDate", scheduledDate);
            model.addAttribute("location", location);
            model.addAttribute("privacySetting", privacySetting);
            model.addAttribute("sessionDuration", sessionDuration);
            // Show friends to invite
            List<Long> friendIds = followService.getFollowingIds(currentUser.getUserId());
            if (!friendIds.isEmpty()) {
                model.addAttribute("friends", friendIds.stream()
                        .map(id -> userService.getUserById(id)).toList());
            }
            return "listings/create-confirm";
        }

        // No positions - go straight to confirm with friends
        model.addAttribute("format", format);
        model.addAttribute("formatId", formatId);
        model.addAttribute("skillLevel", skillLevel);
        model.addAttribute("scheduledDate", scheduledDate);
        model.addAttribute("location", location);
        model.addAttribute("privacySetting", privacySetting);
        model.addAttribute("sessionDuration", sessionDuration);
        List<Long> friendIds = followService.getFollowingIds(currentUser.getUserId());
        if (!friendIds.isEmpty()) {
            model.addAttribute("friends", friendIds.stream()
                    .map(id -> userService.getUserById(id)).toList());
        }
        return "listings/create-confirm";
    }

    // ===== A100: Confirm and Create Listing =====

    @PostMapping("/listings/confirm")
    public String confirmCreate(@AuthenticationPrincipal CustomUserDetails currentUser,
                                @RequestParam Long formatId,
                                @RequestParam String skillLevel,
                                @RequestParam String scheduledDate,
                                @RequestParam String location,
                                @RequestParam String privacySetting,
                                @RequestParam Integer sessionDuration,
                                @RequestParam(required = false) List<Long> positionIds,
                                @RequestParam(required = false) List<Long> invitedFriendIds,
                                RedirectAttributes redirectAttributes) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(scheduledDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            SkillLevel skill = SkillLevel.valueOf(skillLevel.toUpperCase());
            PrivacySetting privacy = PrivacySetting.valueOf(privacySetting.toUpperCase());

            gameListingService.createListing(
                    currentUser.getUserId(), formatId, skill, dateTime, location, privacy,
                    sessionDuration, positionIds, invitedFriendIds);

            redirectAttributes.addFlashAttribute("success", "Game listing created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/listings";
    }

    // ===== View Listing Detail =====

    @GetMapping("/listings/{id}")
    public String viewListing(@PathVariable Long id,
                              @AuthenticationPrincipal CustomUserDetails currentUser,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        GameListing listing = gameListingService.getListingDetail(id);

        boolean isCreator = listing.getCreator().getUserId().equals(currentUser.getUserId());

        // Rule 4: Private listings only accessible to creator or participants/invited users
        if (listing.getPrivacySetting() == PrivacySetting.PRIVATE && !isCreator) {
            // Check if user is a participant (accepted/pending joiner)
            boolean isParticipant = listing.getJoiners().stream()
                    .anyMatch(j -> j.getUser().getUserId().equals(currentUser.getUserId()));
            if (!isParticipant) {
                redirectAttributes.addFlashAttribute("error",
                        "Private listings are only accessible through invitations.");
                return "redirect:/listings";
            }
        }

        // Rule 7: Validate sport is on user's profile (unless creator)
        if (!isCreator) {
            try {
                gameListingService.validateSportProfileAccess(currentUser.getUserId(), listing);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", e.getMessage());
                return "redirect:/listings";
            }
        }

        model.addAttribute("listing", listing);
        model.addAttribute("isCreator", isCreator);

        // Capacity information for UI
        int maxPlayers = listing.getFormat().getNoPlayers();
        long currentParticipants = gameJoinerService.countCurrentParticipants(id);
        boolean listingFull = currentParticipants >= maxPlayers;
        model.addAttribute("listingFull", listingFull);
        model.addAttribute("currentParticipants", currentParticipants);
        model.addAttribute("maxPlayers", maxPlayers);

        // Pass join request status for non-creators so the template can show appropriate UI
        if (!isCreator) {
            JoinerStatus joinStatus = gameJoinerService.getUserJoinRequestStatus(
                    currentUser.getUserId(), id);
            model.addAttribute("joinStatus", joinStatus);
        }

        return "listings/detail";
    }

    // ===== C300: Edit Listing Form =====

    @GetMapping("/listings/{id}/edit")
    public String showEditListing(@PathVariable Long id,
                                  @AuthenticationPrincipal CustomUserDetails currentUser,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        GameListing listing = gameListingService.getListingWithDetails(id);
        if (!listing.getCreator().getUserId().equals(currentUser.getUserId())) {
            redirectAttributes.addFlashAttribute("error", "You can only edit your own listings.");
            return "redirect:/lobby/created";
        }
        model.addAttribute("listing", listing);
        model.addAttribute("skillLevels", SkillLevel.values());
        model.addAttribute("privacySettings", PrivacySetting.values());
        return "listings/edit";
    }

    // ===== C300: Update Listing =====

    @PostMapping("/listings/{id}/edit")
    public String updateListing(@PathVariable Long id,
                                @AuthenticationPrincipal CustomUserDetails currentUser,
                                @RequestParam(required = false) String scheduledDate,
                                @RequestParam(required = false) String location,
                                @RequestParam(required = false) String skillLevel,
                                @RequestParam(required = false) String privacySetting,
                                RedirectAttributes redirectAttributes) {
        try {
            LocalDateTime dateTime = (scheduledDate != null && !scheduledDate.isBlank())
                    ? LocalDateTime.parse(scheduledDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
            SkillLevel skill = (skillLevel != null && !skillLevel.isBlank())
                    ? SkillLevel.valueOf(skillLevel.toUpperCase()) : null;
            PrivacySetting privacy = (privacySetting != null && !privacySetting.isBlank())
                    ? PrivacySetting.valueOf(privacySetting.toUpperCase()) : null;

            gameListingService.updateListing(id, currentUser.getUserId(), dateTime, location, skill, privacy);
            redirectAttributes.addFlashAttribute("success", "Listing updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/listings/" + id;
    }

    // ===== C300: Delete Listing =====

    @PostMapping("/listings/{id}/delete")
    public String deleteListing(@PathVariable Long id,
                                @AuthenticationPrincipal CustomUserDetails currentUser,
                                RedirectAttributes redirectAttributes) {
        try {
            gameListingService.deleteListing(id, currentUser.getUserId());
            redirectAttributes.addFlashAttribute("success", "Listing deleted. All joiners have been notified.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lobby/created";
    }
}
