package com.gameon.controller;

import com.gameon.model.entity.GameListing;
import com.gameon.model.entity.MatchResult;
import com.gameon.security.CustomUserDetails;
import com.gameon.service.GameListingService;
import com.gameon.service.MatchResultService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller for C100 (Record Match Result), C200 (Update Match Result), C400 (View Match Results).
 * Only the listing creator can record/update results.
 */
@Controller
@RequestMapping("/match-result")
public class MatchResultController {

    private final MatchResultService matchResultService;
    private final GameListingService gameListingService;

    public MatchResultController(MatchResultService matchResultService,
                                 GameListingService gameListingService) {
        this.matchResultService = matchResultService;
        this.gameListingService = gameListingService;
    }

    // ===== C100: Show Submit Score Form =====

    @GetMapping("/submit/{listingId}")
    public String showSubmitForm(@PathVariable Long listingId,
                                 @AuthenticationPrincipal CustomUserDetails currentUser,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        GameListing listing = gameListingService.getListingWithDetails(listingId);

        if (!listing.getCreator().getUserId().equals(currentUser.getUserId())) {
            redirectAttributes.addFlashAttribute("error", "Only the listing creator can submit a match result.");
            return "redirect:/lobby/created";
        }

        // Check if result already exists (redirect to update)
        MatchResult existingResult = matchResultService.getResultForListing(listingId);
        if (existingResult != null) {
            model.addAttribute("result", existingResult);
            model.addAttribute("listing", listing);
            model.addAttribute("isUpdate", true);
            return "match-result/form";
        }

        try {
            matchResultService.validateResultWindow(listing);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/lobby/created";
        }

        model.addAttribute("listing", listing);
        model.addAttribute("isUpdate", false);
        return "match-result/form";
    }

    // ===== C100: Submit Match Result =====

    @PostMapping("/submit/{listingId}")
    public String submitResult(@PathVariable Long listingId,
                               @RequestParam int teamAScore,
                               @RequestParam int teamBScore,
                               @AuthenticationPrincipal CustomUserDetails currentUser,
                               RedirectAttributes redirectAttributes) {
        try {
            matchResultService.recordResult(listingId, currentUser.getUserId(), teamAScore, teamBScore);
            redirectAttributes.addFlashAttribute("success",
                    "Match result recorded! Stats have been updated for all participants.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/match-result/submit/" + listingId;
        }
        return "redirect:/lobby/history";
    }

    // ===== C200: Update Match Result =====

    @PostMapping("/update/{listingId}")
    public String updateResult(@PathVariable Long listingId,
                               @RequestParam int teamAScore,
                               @RequestParam int teamBScore,
                               @AuthenticationPrincipal CustomUserDetails currentUser,
                               RedirectAttributes redirectAttributes) {
        try {
            matchResultService.updateResult(listingId, currentUser.getUserId(), teamAScore, teamBScore);
            redirectAttributes.addFlashAttribute("success", "Match result updated! Stats recalculated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/match-result/submit/" + listingId;
        }
        return "redirect:/lobby/history";
    }

    // ===== C400: View Match Results (History) =====

    @GetMapping("/history")
    public String matchHistory(@AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        List<MatchResult> results = matchResultService.getMatchHistory(currentUser.getUserId());
        model.addAttribute("results", results);
        return "match-result/history";
    }

    // ===== View Single Match Result =====

    @GetMapping("/view/{listingId}")
    public String viewResult(@PathVariable Long listingId, Model model,
                             RedirectAttributes redirectAttributes) {
        MatchResult result = matchResultService.getResultForListing(listingId);
        if (result == null) {
            redirectAttributes.addFlashAttribute("error", "No match result found for this listing.");
            return "redirect:/lobby/history";
        }
        GameListing listing = gameListingService.getListingWithDetails(listingId);
        model.addAttribute("result", result);
        model.addAttribute("listing", listing);
        return "match-result/view";
    }
}
