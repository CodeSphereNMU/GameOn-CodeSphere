package com.gameon.controller;

import com.gameon.model.entity.GameListing;
import com.gameon.model.enums.Team;
import com.gameon.security.CustomUserDetails;
import com.gameon.service.GameJoinerService;
import com.gameon.service.GameListingService;
import com.gameon.service.SportService;
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

    public GameJoinerController(GameJoinerService gameJoinerService,
                                GameListingService gameListingService,
                                SportService sportService) {
        this.gameJoinerService = gameJoinerService;
        this.gameListingService = gameListingService;
        this.sportService = sportService;
    }

    // ===== A300: Show Join Form (select team + position) =====

    @GetMapping("/join/{listingId}")
    public String showJoinForm(@PathVariable Long listingId,
                               @AuthenticationPrincipal CustomUserDetails currentUser,
                               Model model) {
        GameListing listing = gameListingService.getListingWithDetails(listingId);
        model.addAttribute("listing", listing);
        model.addAttribute("teams", Team.values());

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
        }
        return "redirect:/listings/" + listingId;
    }

    // ===== A400: Leave Listing =====

    @PostMapping("/leave/{listingId}")
    public String leaveListing(@PathVariable Long listingId,
                               @AuthenticationPrincipal CustomUserDetails currentUser,
                               RedirectAttributes redirectAttributes) {
        try {
            gameJoinerService.leaveListing(currentUser.getUserId(), listingId);
            redirectAttributes.addFlashAttribute("success", "You have left the game listing.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lobby/joined";
    }
}
