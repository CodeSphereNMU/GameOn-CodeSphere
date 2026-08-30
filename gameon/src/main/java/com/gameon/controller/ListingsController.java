package com.gameon.controller;

import com.gameon.model.entity.GameListing;
import com.gameon.model.entity.SportFormat;
import com.gameon.model.dto.CreateListingDraft;
import com.gameon.model.enums.PrivacySetting;
import com.gameon.model.enums.SkillLevel;
import com.gameon.exception.BusinessRuleException;
import com.gameon.security.CustomUserDetails;
import com.gameon.service.FollowService;
import com.gameon.service.GameJoinerService;
import com.gameon.service.GameListingService;
import com.gameon.service.SportService;
import com.gameon.service.UserService;
import com.gameon.service.InvitationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

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
    private final InvitationService invitationService;

    public ListingsController(GameListingService gameListingService,
                              SportService sportService,
                              FollowService followService,
                              UserService userService,
                              GameJoinerService gameJoinerService,
                              InvitationService invitationService) {
        this.gameListingService = gameListingService;
        this.sportService = sportService;
        this.followService = followService;
        this.userService = userService;
        this.gameJoinerService = gameJoinerService;
        this.invitationService = invitationService;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/listings";
    }

    // ===== A200: Browse Available Listings (PUBLIC only) =====

    @GetMapping("/listings")
    public String index(@AuthenticationPrincipal CustomUserDetails currentUser,
                        @RequestParam(required = false) String skill,
                        @RequestParam(required = false) Long sportId,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        @RequestParam(defaultValue = "false") boolean hideFull,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        Page<GameListing> listings = Page.empty();
        try {
            SkillLevel selectedSkill = skill == null || skill.isBlank()
                    ? null : SkillLevel.valueOf(skill.toUpperCase());
            listings = gameListingService.browseAvailableListings(
                    currentUser.getUserId(), sportId, selectedSkill, date, hideFull, PageRequest.of(page, 12));
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }

        model.addAttribute("listings", listings);
        model.addAttribute("skillLevels", SkillLevel.values());
        List<SportFormat> userFormats = sportService.getFormatsForUserSports(currentUser.getUserId());
        model.addAttribute("userSports", userFormats.stream().map(SportFormat::getSport)
                .collect(java.util.stream.Collectors.toMap(
                        sport -> sport.getSportId(), sport -> sport, (first, duplicate) -> first))
                .values());
        model.addAttribute("selectedSkill", skill);
        model.addAttribute("selectedSportId", sportId);
        model.addAttribute("selectedDate", date);
        model.addAttribute("hideFull", hideFull);
        Map<Long, Long> participantCounts = new LinkedHashMap<>();
        listings.forEach(listing -> participantCounts.put(
                listing.getGameListingId(),
                gameJoinerService.countCurrentParticipants(listing.getGameListingId())));
        model.addAttribute("participantCounts", participantCounts);
        return "listings/index";
    }

    // ===== A100: Create Game Listing (Step 1 - Form) =====

    @GetMapping("/listings/create")
    public String showCreateForm(@AuthenticationPrincipal CustomUserDetails currentUser,
                                 HttpSession session, Model model) {
        populateCreateForm(currentUser.getUserId(), session, model);
        return "listings/create";
    }

    // ===== A100: Create Game Listing (Step 2 - Positions, if applicable) =====

    @PostMapping("/listings/create")
    public String processCreate(@AuthenticationPrincipal CustomUserDetails currentUser,
                                @RequestParam Long sportId,
                                @RequestParam Long formatId,
                                @RequestParam String skillLevel,
                                @RequestParam String scheduledDate,
                                @RequestParam String location,
                                @RequestParam String privacySetting,
                                HttpSession session,
                                Model model) {
        try {
            // Validate format belongs to the selected sport
            SportFormat selectedFormat = sportService.getFormatById(formatId);
            if (!selectedFormat.getSport().getSportId().equals(sportId)) {
                throw new BusinessRuleException(
                        "Selected format does not belong to the chosen sport.");
            }

            LocalDateTime dateTime = LocalDateTime.parse(scheduledDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            SkillLevel skill = SkillLevel.valueOf(skillLevel.toUpperCase());
            PrivacySetting privacy = PrivacySetting.valueOf(privacySetting.toUpperCase());
            Integer durationMinutes = selectedFormat.getDurationMinutes();
            SportFormat format = gameListingService.validateListingDetails(
                    currentUser.getUserId(), formatId, skill, dateTime, location, privacy, durationMinutes);

            CreateListingDraft draft = new CreateListingDraft();
            draft.setFormatId(formatId);
            draft.setSkillLevel(skill);
            draft.setScheduledDate(dateTime);
            draft.setLocation(location.trim());
            draft.setPrivacySetting(privacy);
            draft.setDurationMinutes(durationMinutes);
            draft.setPositionIds(format.getHasPositions() ? null : new ArrayList<>());
            session.setAttribute("listingDraft", draft);

            return format.getHasPositions()
                    ? "redirect:/listings/create/positions"
                    : "redirect:/listings/create/friends";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("submittedSportId", sportId);
            model.addAttribute("submittedFormatId", formatId);
            model.addAttribute("submittedSkillLevel", skillLevel);
            model.addAttribute("submittedScheduledDate", scheduledDate);
            model.addAttribute("submittedLocation", location);
            model.addAttribute("submittedPrivacySetting", privacySetting);
            populateCreateForm(currentUser.getUserId(), session, model);
            return "listings/create";
        }
    }

    @GetMapping("/listings/create/positions")
    public String showCreatePositions(HttpSession session, Model model) {
        CreateListingDraft draft = getDraft(session);
        if (draft == null) return "redirect:/listings/create";
        SportFormat format = sportService.getFormatById(draft.getFormatId());
        if (!format.getHasPositions()) return "redirect:/listings/create/friends";
        model.addAttribute("format", format);
        model.addAttribute("positions", sportService.getPositionsForFormat(draft.getFormatId()));
        model.addAttribute("draft", draft);
        return "listings/create-positions";
    }

    @PostMapping("/listings/create/positions")
    public String processCreatePositions(@RequestParam(defaultValue = "false") boolean anyPosition,
                                         @RequestParam(required = false) List<Long> positionIds,
                                         HttpSession session, Model model) {
        CreateListingDraft draft = getDraft(session);
        if (draft == null) return "redirect:/listings/create";
        SportFormat format = sportService.getFormatById(draft.getFormatId());
        List<Long> selected = anyPosition ? new ArrayList<>() : positionIds;
        try {
            gameListingService.validatePositionSelection(format, selected);
            draft.setPositionIds(selected);
            return "redirect:/listings/create/friends";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("format", format);
            model.addAttribute("positions", sportService.getPositionsForFormat(draft.getFormatId()));
            model.addAttribute("draft", draft);
            model.addAttribute("submittedAnyPosition", anyPosition);
            model.addAttribute("submittedPositionIds", positionIds);
            return "listings/create-positions";
        }
    }

    @GetMapping("/listings/create/friends")
    public String showCreateFriends(@AuthenticationPrincipal CustomUserDetails currentUser,
                                    HttpSession session, Model model) {
        CreateListingDraft draft = getDraft(session);
        if (draft == null) return "redirect:/listings/create";
        SportFormat format = sportService.getFormatById(draft.getFormatId());
        if (format.getHasPositions() && draft.getPositionIds() == null) {
            return "redirect:/listings/create/positions";
        }
        List<Long> friendIds = followService.getFriendIds(currentUser.getUserId());
        model.addAttribute("friends", friendIds.stream().map(userService::getUserById).toList());
        model.addAttribute("draft", draft);
        model.addAttribute("format", format);
        return "listings/create-friends";
    }

    @PostMapping("/listings/create/friends")
    public String processCreateFriends(@AuthenticationPrincipal CustomUserDetails currentUser,
                                       @RequestParam(required = false) List<Long> invitedFriendIds,
                                       HttpSession session, RedirectAttributes redirectAttributes) {
        CreateListingDraft draft = getDraft(session);
        if (draft == null) return "redirect:/listings/create";
        List<Long> friendIds = followService.getFriendIds(currentUser.getUserId());
        if (invitedFriendIds != null && !friendIds.containsAll(invitedFriendIds)) {
            redirectAttributes.addFlashAttribute("error", "You can only invite friends.");
            return "redirect:/listings/create/friends";
        }
        draft.setInvitedFriendIds(invitedFriendIds);
        return "redirect:/listings/create/preview";
    }

    @GetMapping("/listings/create/preview")
    public String showCreatePreview(HttpSession session, Model model) {
        CreateListingDraft draft = getDraft(session);
        if (draft == null) return "redirect:/listings/create";
        SportFormat format = sportService.getFormatById(draft.getFormatId());
        if (format.getHasPositions() && draft.getPositionIds() == null) {
            return "redirect:/listings/create/positions";
        }
        populatePreview(draft, model);
        return "listings/create-confirm";
    }

    // ===== A100: Confirm and Create Listing =====

    @PostMapping("/listings/confirm")
    public String confirmCreate(@AuthenticationPrincipal CustomUserDetails currentUser,
                                HttpSession session, Model model,
                                RedirectAttributes redirectAttributes) {
        CreateListingDraft draft = getDraft(session);
        if (draft == null) return "redirect:/listings/create";
        try {
            gameListingService.createListing(
                    currentUser.getUserId(), draft.getFormatId(), draft.getSkillLevel(),
                    draft.getScheduledDate(), draft.getLocation(), draft.getPrivacySetting(),
                    draft.getDurationMinutes(), draft.getPositionIds(), draft.getInvitedFriendIds());

            session.removeAttribute("listingDraft");
            redirectAttributes.addFlashAttribute("success", "Game listing created successfully!");
            return "redirect:/listings";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            populatePreview(draft, model);
            return "listings/create-confirm";
        }
    }

    private void populateCreateForm(Long userId, HttpSession session, Model model) {
        List<SportFormat> formats = sportService.getFormatsForUserSports(userId);
        model.addAttribute("formats", formats);
        // Build distinct sports list for the Sport dropdown
        model.addAttribute("sports", formats.stream()
                .map(SportFormat::getSport)
                .collect(java.util.stream.Collectors.toMap(
                        sport -> sport.getSportId(), sport -> sport, (first, dup) -> first))
                .values());
        model.addAttribute("skillLevels", SkillLevel.values());
        model.addAttribute("privacySettings", PrivacySetting.values());
        CreateListingDraft draft = getDraft(session);
        model.addAttribute("draft", draft);
        // Derive selectedSportId from existing draft format for pre-selection
        if (draft != null && draft.getFormatId() != null) {
            SportFormat draftFormat = sportService.getFormatById(draft.getFormatId());
            model.addAttribute("draftSportId", draftFormat.getSport().getSportId());
        }
    }

    private void populatePreview(CreateListingDraft draft, Model model) {
        model.addAttribute("draft", draft);
        SportFormat format = sportService.getFormatById(draft.getFormatId());
        model.addAttribute("format", format);
        if (format.getHasPositions()) {
            model.addAttribute("positionNames", sportService.getPositionNamesForFormat(draft.getFormatId()));
        }
    }

    private CreateListingDraft getDraft(HttpSession session) {
        Object value = session.getAttribute("listingDraft");
        return value instanceof CreateListingDraft ? (CreateListingDraft) value : null;
    }

    @GetMapping("/listings/create/cancel")
    public String cancelCreate(HttpSession session) {
        session.removeAttribute("listingDraft");
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
            boolean isInvited = invitationService.isInvited(id, currentUser.getUserId());
            if (!isParticipant && !isInvited) {
                redirectAttributes.addFlashAttribute("error",
                        "Private listings are only accessible through invitations.");
                return "redirect:/listings";
            }
        }

        // Rule 7: Validate sport is on user's profile (unless creator)
        if (!isCreator && !invitationService.isInvited(id, currentUser.getUserId())) {
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
        model.addAttribute("requestWindowOpen", gameJoinerService.isRequestWindowOpen(listing));
        listing.getJoiners().stream()
                .filter(joiner -> joiner.getUser().getUserId().equals(listing.getCreator().getUserId()))
                .findFirst()
                .ifPresent(joiner -> model.addAttribute("creatorJoiner", joiner));
        model.addAttribute("teamACount", gameJoinerService.getTeamCount(id, com.gameon.model.enums.Team.A));
        model.addAttribute("teamBCount", gameJoinerService.getTeamCount(id, com.gameon.model.enums.Team.B));
        model.addAttribute("teamCapacity", maxPlayers / 2);
        if (listing.getFormat().getHasPositions()) {
            model.addAttribute("positionNames",
                    sportService.getPositionNamesForFormat(listing.getFormat().getFormatId()));
        }

        // Join requests and actual participants are separate records.
        if (!isCreator) {
            String joinState = gameJoinerService.hasPendingRequest(currentUser.getUserId(), id)
                    ? "PENDING"
                    : gameJoinerService.isParticipant(currentUser.getUserId(), id) ? "PARTICIPANT" : null;
            model.addAttribute("joinState", joinState);

            // Attendance confirmation state for participant
            if ("PARTICIPANT".equals(joinState)) {
                listing.getJoiners().stream()
                        .filter(j -> j.getUser().getUserId().equals(currentUser.getUserId()))
                        .findFirst()
                        .ifPresent(joiner -> {
                            model.addAttribute("myJoinerStatus", joiner.getStatus().name());
                            model.addAttribute("confirmationAvailable",
                                    joiner.getStatus().name().equals("ACCEPTED")
                                            && gameJoinerService.isConfirmationAvailable(listing));
                        });
            }

            // Check if user has a last-call offer
            if ("PENDING".equals(joinState)) {
                model.addAttribute("hasLastCallOffer",
                        gameJoinerService.hasLastCallOffer(currentUser.getUserId(), id));
            }
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
        try {
            gameListingService.validateEditable(listing);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/listings/" + id;
        }
        model.addAttribute("listing", listing);
        model.addAttribute("skillLevels", SkillLevel.values());
        model.addAttribute("privacySettings", PrivacySetting.values());
        // Cancel is only shown while the listing is still cancellable (more than 1 hour before start).
        model.addAttribute("cancellable", gameListingService.isCreatorCancellable(listing));
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

    // ===== C300: Cancel Listing =====

    @PostMapping("/listings/{id}/cancel")
    public String deleteListing(@PathVariable Long id,
                                @AuthenticationPrincipal CustomUserDetails currentUser,
                                RedirectAttributes redirectAttributes) {
        try {
            gameListingService.cancelListing(id, currentUser.getUserId());
            redirectAttributes.addFlashAttribute("success", "Listing cancelled. Affected users have been notified.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lobby/created";
    }
}
