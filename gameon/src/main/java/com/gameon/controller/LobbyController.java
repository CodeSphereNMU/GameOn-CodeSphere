package com.gameon.controller;

import com.gameon.model.entity.GameJoiner;
import com.gameon.model.entity.GameListing;
import com.gameon.model.entity.MatchResult;
import com.gameon.model.enums.JoinerStatus;
import com.gameon.model.enums.Team;
import com.gameon.security.CustomUserDetails;
import com.gameon.service.GameJoinerService;
import com.gameon.service.GameListingService;
import com.gameon.service.MatchResultService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller for the Lobby tab.
 * Created Listings tab, Joined Listings tab, Match History tab.
 * Also handles C500 (Accept/Reject join requests).
 */
@Controller
@RequestMapping("/lobby")
public class LobbyController {

    private final GameListingService gameListingService;
    private final GameJoinerService gameJoinerService;
    private final MatchResultService matchResultService;

    public LobbyController(GameListingService gameListingService,
                           GameJoinerService gameJoinerService,
                           MatchResultService matchResultService) {
        this.gameListingService = gameListingService;
        this.gameJoinerService = gameJoinerService;
        this.matchResultService = matchResultService;
    }

    // ===== Lobby - Default (Created Tab) =====

    @GetMapping
    public String lobby(@AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        return created(currentUser, model);
    }

    // ===== Lobby - Created Listings =====

    @GetMapping("/created")
    public String created(@AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        List<GameListing> listings = gameListingService.getCreatedListings(currentUser.getUserId());
        model.addAttribute("listings", listings);
        model.addAttribute("activeTab", "created");
        return "lobby/index";
    }

    // ===== Lobby - Joined Listings =====

    @GetMapping("/joined")
    public String joined(@AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        List<GameJoiner> joinedGames = gameJoinerService.getJoinedListings(currentUser.getUserId());
        model.addAttribute("joinedGames", joinedGames);
        model.addAttribute("activeTab", "joined");
        return "lobby/index";
    }

    // ===== Lobby - Match History =====

    @GetMapping("/history")
    public String history(@AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        List<MatchResult> results = matchResultService.getMatchHistory(currentUser.getUserId());
        model.addAttribute("results", results);
        model.addAttribute("activeTab", "history");
        return "lobby/index";
    }

    // ===== C500: View Join Requests for a Listing =====

    @GetMapping("/requests/{listingId}")
    public String viewRequests(@PathVariable Long listingId,
                               @AuthenticationPrincipal CustomUserDetails currentUser,
                               Model model) {
        GameListing listing = gameListingService.getListingWithDetails(listingId);
        // Verify ownership
        if (!listing.getCreator().getUserId().equals(currentUser.getUserId())) {
            return "redirect:/lobby";
        }

        List<GameJoiner> pendingRequests = gameJoinerService.getPendingRequests(listingId);
        List<GameJoiner> acceptedJoiners = gameJoinerService.getJoinersByStatus(listingId, JoinerStatus.ACCEPTED);

        // Capacity information for UI
        int maxPlayers = listing.getFormat().getNoPlayers();
        long currentParticipants = gameJoinerService.countCurrentParticipants(listingId);
        boolean listingFull = currentParticipants >= maxPlayers;
        int teamCapacity = maxPlayers / 2;
        boolean teamAFull = gameJoinerService.isTeamFull(listingId, Team.A, maxPlayers);
        boolean teamBFull = gameJoinerService.isTeamFull(listingId, Team.B, maxPlayers);

        model.addAttribute("listing", listing);
        model.addAttribute("pendingRequests", pendingRequests);
        model.addAttribute("acceptedJoiners", acceptedJoiners);
        model.addAttribute("listingFull", listingFull);
        model.addAttribute("currentParticipants", currentParticipants);
        model.addAttribute("maxPlayers", maxPlayers);
        model.addAttribute("teamCapacity", teamCapacity);
        model.addAttribute("teamAFull", teamAFull);
        model.addAttribute("teamBFull", teamBFull);
        return "lobby/requests";
    }

    // ===== C500: Accept Join Request =====

    @PostMapping("/requests/{listingId}/accept/{userId}")
    public String acceptRequest(@PathVariable Long listingId,
                                @PathVariable Long userId,
                                @AuthenticationPrincipal CustomUserDetails currentUser,
                                RedirectAttributes redirectAttributes) {
        try {
            gameJoinerService.acceptRequest(listingId, userId, currentUser.getUserId());
            redirectAttributes.addFlashAttribute("success", "Join request accepted!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lobby/requests/" + listingId;
    }

    // ===== C500: Reject Join Request =====

    @PostMapping("/requests/{listingId}/reject/{userId}")
    public String rejectRequest(@PathVariable Long listingId,
                                @PathVariable Long userId,
                                @AuthenticationPrincipal CustomUserDetails currentUser,
                                RedirectAttributes redirectAttributes) {
        try {
            gameJoinerService.rejectRequest(listingId, userId, currentUser.getUserId());
            redirectAttributes.addFlashAttribute("success", "Join request rejected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lobby/requests/" + listingId;
    }
}
