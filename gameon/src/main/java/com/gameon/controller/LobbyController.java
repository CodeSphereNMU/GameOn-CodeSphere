package com.gameon.controller;

import com.gameon.model.entity.GameJoiner;
import com.gameon.model.entity.GameListing;
import com.gameon.model.entity.JoinRequest;
import com.gameon.model.enums.JoinerStatus;
import com.gameon.model.enums.Team;
import com.gameon.security.CustomUserDetails;
import com.gameon.service.GameJoinerService;
import com.gameon.service.GameListingService;
import com.gameon.service.ListingLifecycleService;
import com.gameon.service.MatchResultService;
import com.gameon.service.SportService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controller for the Lobby tab.
 * Created Listings tab, Joined Listings tab, Match History tab.
 * Also handles C500 (Accept/Reject join requests) and last-call selection.
 */
@Controller
@RequestMapping("/lobby")
public class LobbyController {

    private final GameListingService gameListingService;
    private final GameJoinerService gameJoinerService;
    private final MatchResultService matchResultService;
    private final SportService sportService;
    private final ListingLifecycleService listingLifecycleService;

    public LobbyController(GameListingService gameListingService,
                           GameJoinerService gameJoinerService,
                           MatchResultService matchResultService,
                           SportService sportService,
                           ListingLifecycleService listingLifecycleService) {
        this.gameListingService = gameListingService;
        this.gameJoinerService = gameJoinerService;
        this.matchResultService = matchResultService;
        this.sportService = sportService;
        this.listingLifecycleService = listingLifecycleService;
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
        model.addAttribute("editableListingIds", listings.stream()
                .filter(gameListingService::isEditable)
                .map(GameListing::getGameListingId)
                .collect(Collectors.toSet()));
        model.addAttribute("requestOpenListingIds", listings.stream()
                .filter(gameJoinerService::isRequestWindowOpen)
                .map(GameListing::getGameListingId)
                .collect(Collectors.toSet()));
        // Listings the creator may still cancel (OPEN and more than 1 hour before start).
        model.addAttribute("cancellableListingIds", listings.stream()
                .filter(gameListingService::isCreatorCancellable)
                .map(GameListing::getGameListingId)
                .collect(Collectors.toSet()));
        // Listings in last-call period (T-2h → T-1h) where creator can manage replacements
        model.addAttribute("lastCallListingIds", listings.stream()
                .filter(listingLifecycleService::isInLastCallPeriod)
                .map(GameListing::getGameListingId)
                .collect(Collectors.toSet()));
        // Listings where confirmation is available (for creator's own confirmation)
        model.addAttribute("confirmationAvailableIds", listings.stream()
                .filter(listingLifecycleService::isConfirmationWindowOpen)
                .map(GameListing::getGameListingId)
                .collect(Collectors.toSet()));
        model.addAttribute("activeTab", "created");
        return "lobby/index";
    }

    // ===== Lobby - Joined Listings =====

    @GetMapping("/joined")
    public String joined(@AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        List<GameJoiner> joinedGames = gameJoinerService.getJoinedListings(currentUser.getUserId());
        model.addAttribute("joinedGames", joinedGames);
        model.addAttribute("pendingRequests",
                gameJoinerService.getPendingRequestsForUser(currentUser.getUserId()));

        // IDs where leaving is possible (before T-1h and listing still open)
        model.addAttribute("leaveOpenListingIds", joinedGames.stream()
                .filter(joiner -> gameJoinerService.isRequestWindowOpen(joiner.getGameListing())
                        || gameJoinerService.isInLastCallPeriod(joiner.getGameListing()))
                .map(joiner -> joiner.getGameListing().getGameListingId())
                .collect(Collectors.toSet()));

        // IDs where attendance confirmation is available
        model.addAttribute("confirmationAvailableIds", joinedGames.stream()
                .filter(joiner -> joiner.getStatus() == JoinerStatus.ACCEPTED)
                .filter(joiner -> listingLifecycleService.isConfirmationWindowOpen(joiner.getGameListing()))
                .map(joiner -> joiner.getGameListing().getGameListingId())
                .collect(Collectors.toSet()));

        // IDs in late-withdrawal period (for warning display)
        model.addAttribute("lateWithdrawalIds", joinedGames.stream()
                .filter(joiner -> listingLifecycleService.isInLastCallPeriod(joiner.getGameListing()))
                .map(joiner -> joiner.getGameListing().getGameListingId())
                .collect(Collectors.toSet()));

        model.addAttribute("activeTab", "joined");
        return "lobby/index";
    }

    // ===== Lobby - Match History =====

    @GetMapping("/history")
    public String history(@AuthenticationPrincipal CustomUserDetails currentUser, Model model) {
        List<GameListing> historyListings = matchResultService.getMatchHistoryListings(currentUser.getUserId());
        model.addAttribute("historyListings", historyListings);
        model.addAttribute("currentUserId", currentUser.getUserId());
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

        List<JoinRequest> pendingRequests = gameJoinerService.getPendingRequests(listingId);
        List<GameJoiner> acceptedJoiners = gameJoinerService.getParticipants(listingId);

        // Capacity information for UI
        int maxPlayers = listing.getFormat().getNoPlayers();
        long currentParticipants = gameJoinerService.countCurrentParticipants(listingId);
        boolean listingFull = currentParticipants >= maxPlayers;
        int teamCapacity = maxPlayers / 2;
        boolean teamAFull = gameJoinerService.isTeamFull(listingId, Team.A, maxPlayers);
        boolean teamBFull = gameJoinerService.isTeamFull(listingId, Team.B, maxPlayers);

        // Lifecycle phase info
        boolean isInLastCallPeriod = listingLifecycleService.isInLastCallPeriod(listing);
        boolean confirmationWindowOpen = listingLifecycleService.isConfirmationWindowOpen(listing);

        model.addAttribute("listing", listing);
        model.addAttribute("pendingRequests", pendingRequests);
        model.addAttribute("acceptedJoiners", acceptedJoiners);
        model.addAttribute("listingFull", listingFull);
        model.addAttribute("currentParticipants", currentParticipants);
        model.addAttribute("maxPlayers", maxPlayers);
        model.addAttribute("teamCapacity", teamCapacity);
        model.addAttribute("teamAFull", teamAFull);
        model.addAttribute("teamBFull", teamBFull);
        model.addAttribute("requestWindowOpen", gameJoinerService.isRequestWindowOpen(listing));
        model.addAttribute("isInLastCallPeriod", isInLastCallPeriod);
        model.addAttribute("confirmationWindowOpen", confirmationWindowOpen);

        if (listing.getFormat().getHasPositions()) {
            model.addAttribute("positionNames",
                    sportService.getPositionNamesForFormat(listing.getFormat().getFormatId()));
        }
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

    // ===== Last-Call: Creator selects multiple users for replacement notification =====

    @PostMapping("/requests/{listingId}/last-call")
    public String approveLastCall(@PathVariable Long listingId,
                                  @RequestParam(required = false) List<Long> selectedUserIds,
                                  @AuthenticationPrincipal CustomUserDetails currentUser,
                                  RedirectAttributes redirectAttributes) {
        if (selectedUserIds == null || selectedUserIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("error",
                    "Please select at least one requester to send a last-call notification.");
            return "redirect:/lobby/requests/" + listingId;
        }
        try {
            gameJoinerService.approveLastCallRequesters(listingId, currentUser.getUserId(), selectedUserIds);
            redirectAttributes.addFlashAttribute("success",
                    "Last-call notifications sent to " + selectedUserIds.size() + " player(s).");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lobby/requests/" + listingId;
    }

    // ===== Creator Confirm Attendance (from Created tab) =====

    @PostMapping("/created/{listingId}/confirm")
    public String creatorConfirmAttendance(@PathVariable Long listingId,
                                           @AuthenticationPrincipal CustomUserDetails currentUser,
                                           RedirectAttributes redirectAttributes) {
        try {
            gameJoinerService.confirmAttendance(currentUser.getUserId(), listingId);
            redirectAttributes.addFlashAttribute("success", "Your attendance has been confirmed.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lobby/created";
    }
}
