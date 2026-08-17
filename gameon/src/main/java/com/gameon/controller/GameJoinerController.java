package com.gameon.controller;

import com.gameon.model.entity.GameListing;
import com.gameon.model.enums.JoinerStatus;
import com.gameon.model.enums.PrivacySetting;
import com.gameon.model.enums.Team;
import com.gameon.security.CustomUserDetails;
import com.gameon.service.GameJoinerService;
import com.gameon.service.GameListingService;
import com.gameon.service.SportService;
import com.gameon.service.InvitationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for A300 (Send Join Request) and A400 (Leave Listing).
 */
@Controller
@RequestMapping("/game-joiner")
public class GameJoinerController {

    private final GameJoinerService gameJoinerService;
    private final GameListingService gameListingService;
    private final SportService sportService;
    private final InvitationService invitationService;

    public GameJoinerController(GameJoinerService gameJoinerService,
                                GameListingService gameListingService,
                                SportService sportService,
                                InvitationService invitationService) {
        this.gameJoinerService = gameJoinerService;
        this.gameListingService = gameListingService;
        this.sportService = sportService;
        this.invitationService = invitationService;
    }

    // ===== A300: Show Join Form (select team + position) =====

    @GetMapping("/join/{listingId}")
    public String showJoinForm(@PathVariable Long listingId,
                               @RequestParam(required = false) Team team,
                               @AuthenticationPrincipal CustomUserDetails currentUser,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        GameListing listing = gameListingService.getListingWithDetails(listingId);

        if (team == null) {
            redirectAttributes.addFlashAttribute("error", "Select the team you want to join.");
            return "redirect:/listings/" + listingId;
        }

        // Rule 7: Validate sport is on user's profile
        if (!invitationService.isInvited(listingId, currentUser.getUserId())) {
            try {
                gameListingService.validateSportProfileAccess(currentUser.getUserId(), listing);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", e.getMessage());
                return "redirect:/listings/" + listingId;
            }
        }

        // Rule 4: Cannot join private listing unless invited (allowed to view join form if they got this far)
        // The user must have accessed the listing detail page first (which enforces privacy)

        // Cannot join own listing
        if (listing.getCreator().getUserId().equals(currentUser.getUserId())) {
            redirectAttributes.addFlashAttribute("error", "You are already participating in this listing as the creator.");
            return "redirect:/listings/" + listingId;
        }

        // Check for existing active join request (PENDING, ACCEPTED, LOCKED)
        JoinerStatus existingStatus = gameJoinerService.getUserJoinRequestStatus(
                currentUser.getUserId(), listingId);
        if (existingStatus != null) {
            String message = switch (existingStatus) {
                case PENDING -> "You already have a join request for this listing and cannot select another team.";
                case ACCEPTED, LOCKED -> "You are already a participant in this listing.";
                default -> "You already have a join request for this listing.";
            };
            redirectAttributes.addFlashAttribute("error", message);
            return "redirect:/listings/" + listingId;
        }

        // Capacity check: reject if listing is full
        int maxPlayers = listing.getFormat().getNoPlayers();
        if (gameJoinerService.isListingFull(listingId, maxPlayers)) {
            redirectAttributes.addFlashAttribute("error", "This listing is full. No more players can join.");
            return "redirect:/listings/" + listingId;
        }

        if (gameJoinerService.isTeamFull(listingId, team, maxPlayers)) {
            redirectAttributes.addFlashAttribute("error", "Team " + team.name() + " is full.");
            return "redirect:/listings/" + listingId;
        }

        try {
            gameJoinerService.validateJoinAvailability(currentUser.getUserId(), listing);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/listings/" + listingId;
        }

        model.addAttribute("listing", listing);
        model.addAttribute("teams", Team.values());
        model.addAttribute("selectedTeam", team);

        // Team capacity info for the form
        int teamCapacity = maxPlayers / 2;
        boolean teamAFull = gameJoinerService.isTeamFull(listingId, Team.A, maxPlayers);
        boolean teamBFull = gameJoinerService.isTeamFull(listingId, Team.B, maxPlayers);
        model.addAttribute("teamCapacity", teamCapacity);
        model.addAttribute("teamAFull", teamAFull);
        model.addAttribute("teamBFull", teamBFull);

        // If format has positions, load them
        if (listing.getFormat().getHasPositions()) {
            model.addAttribute("positions", sportService.getPositionsForFormat(listing.getFormat().getFormatId()));
        }

        return "game-joiner/join";
    }

    // ===== A300: Submit Join Request =====

    @PostMapping("/join/{listingId}")
    public String submitJoinRequest(@PathVariable Long listingId,
                                    @RequestParam Team team,
                                    @RequestParam(required = false) Long formatPositionId,
                                    @RequestParam(required = false) Long altFormatPositionId,
                                    @AuthenticationPrincipal CustomUserDetails currentUser,
                                    RedirectAttributes redirectAttributes) {
        try {
            gameJoinerService.sendJoinRequest(
                    currentUser.getUserId(), listingId, team, formatPositionId, altFormatPositionId);
            redirectAttributes.addFlashAttribute("success", "Join request sent! The creator will review it.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/game-joiner/join/" + listingId + "?team=" + team.name();
        }
        return "redirect:/listings/" + listingId;
    }

    // ===== A400: Leave Listing =====

    @PostMapping("/leave/{listingId}")
    public String leaveListing(@PathVariable Long listingId,
                               @AuthenticationPrincipal CustomUserDetails currentUser,
                               RedirectAttributes redirectAttributes) {
        try {
            JoinerStatus currentStatus = gameJoinerService.getUserJoinRequestStatus(
                    currentUser.getUserId(), listingId);
            gameJoinerService.leaveListing(currentUser.getUserId(), listingId);
            redirectAttributes.addFlashAttribute("success",
                    currentStatus == JoinerStatus.PENDING
                            ? "Your join request has been withdrawn."
                            : "You have left the game listing.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lobby/joined";
    }
}
