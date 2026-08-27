package com.gameon.controller;

import com.gameon.model.entity.GameListing;
import com.gameon.model.entity.SportFormat;
import com.gameon.model.enums.JoinerStatus;
import com.gameon.model.enums.PrivacySetting;
import com.gameon.model.enums.SkillLevel;
import com.gameon.model.dto.WeatherDTO;
import com.gameon.security.CustomUserDetails;
import com.gameon.service.FollowService;
import com.gameon.service.GameJoinerService;
import com.gameon.service.GameListingService;
import com.gameon.service.InvitationService;
import com.gameon.service.SportService;
import com.gameon.service.UserService;
import com.gameon.service.WeatherService;
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

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ListingsController.class);

    private final GameListingService gameListingService;
    private final SportService sportService;
    private final FollowService followService;
    private final UserService userService;
    private final GameJoinerService gameJoinerService;
    private final InvitationService invitationService;
    private final WeatherService weatherService;

    public ListingsController(GameListingService gameListingService,
                              SportService sportService,
                              FollowService followService,
                              UserService userService,
                              GameJoinerService gameJoinerService,
                              InvitationService invitationService,
                              WeatherService weatherService) {
        this.gameListingService = gameListingService;
        this.sportService = sportService;
        this.followService = followService;
        this.userService = userService;
        this.gameJoinerService = gameJoinerService;
        this.invitationService = invitationService;
        this.weatherService = weatherService;
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
                                @RequestParam(required = false) String venueName,
                                @RequestParam(required = false) String address,
                                @RequestParam(required = false) Double latitude,
                                @RequestParam(required = false) Double longitude,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        // Debug: Log location values received from the form
        logger.info("[Create Listing Step 1] Location values received - venueName={}, address={}, lat={}, lng={}, location={}",
                venueName, address, latitude, longitude, location);

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
            model.addAttribute("venueName", venueName);
            model.addAttribute("address", address);
            model.addAttribute("latitude", latitude);
            model.addAttribute("longitude", longitude);
            // Show friends to invite
            List<Long> friendIds = followService.getFollowingIds(currentUser.getUserId());
            if (!friendIds.isEmpty()) {
                model.addAttribute("friends", friendIds.stream()
                        .map(id -> userService.getUserById(id)).toList());
            }
            // Fetch weather forecast for confirmation preview
            if (latitude != null && longitude != null && scheduledDate != null && !scheduledDate.isBlank()) {
                try {
                    LocalDateTime dateTime = LocalDateTime.parse(scheduledDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    WeatherDTO weather = weatherService.getForecast(latitude, longitude, dateTime);
                    model.addAttribute("weather", weather);
                } catch (Exception e) {
                    logger.debug("Weather fetch failed for confirm page: {}", e.getMessage());
                }
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
        model.addAttribute("venueName", venueName);
        model.addAttribute("address", address);
        model.addAttribute("latitude", latitude);
        model.addAttribute("longitude", longitude);
        List<Long> friendIds = followService.getFollowingIds(currentUser.getUserId());
        if (!friendIds.isEmpty()) {
            model.addAttribute("friends", friendIds.stream()
                    .map(id -> userService.getUserById(id)).toList());
        }
        // Fetch weather forecast for confirmation preview
        if (latitude != null && longitude != null && scheduledDate != null && !scheduledDate.isBlank()) {
            try {
                LocalDateTime dateTime = LocalDateTime.parse(scheduledDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                WeatherDTO weather = weatherService.getForecast(latitude, longitude, dateTime);
                model.addAttribute("weather", weather);
            } catch (Exception e) {
                logger.debug("Weather fetch failed for confirm page: {}", e.getMessage());
            }
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
                                @RequestParam(required = false) String venueName,
                                @RequestParam(required = false) String address,
                                @RequestParam(required = false) Double latitude,
                                @RequestParam(required = false) Double longitude,
                                @RequestParam(required = false) List<Long> positionIds,
                                @RequestParam(required = false) List<Long> invitedFriendIds,
                                RedirectAttributes redirectAttributes) {
        // Debug: Log location values received from confirm form
        logger.info("[Create Listing Confirm] Location values received - venueName={}, address={}, lat={}, lng={}, location={}",
                venueName, address, latitude, longitude, location);

        try {
            LocalDateTime dateTime = LocalDateTime.parse(scheduledDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            SkillLevel skill = SkillLevel.valueOf(skillLevel.toUpperCase());
            PrivacySetting privacy = PrivacySetting.valueOf(privacySetting.toUpperCase());

            // Validate coordinates if provided
            if (latitude != null && longitude != null) {
                if (latitude < -90 || latitude > 90) {
                    throw new IllegalArgumentException("Latitude must be between -90 and 90.");
                }
                if (longitude < -180 || longitude > 180) {
                    throw new IllegalArgumentException("Longitude must be between -180 and 180.");
                }
            }

            GameListing createdListing = gameListingService.createListing(
                    currentUser.getUserId(), formatId, skill, dateTime, location, privacy,
                    sessionDuration, positionIds, invitedFriendIds,
                    venueName, address, latitude, longitude);

            // Fetch and store weather forecast for the new listing
            try {
                weatherService.fetchAndStoreWeather(createdListing);
            } catch (Exception we) {
                logger.debug("Weather fetch failed for new listing {}: {}", createdListing.getGameListingId(), we.getMessage());
            }

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

        // Check if listing is OPEN (for invite button visibility)
        boolean isOpen = invitationService.isListingOpen(listing);
        model.addAttribute("isOpen", isOpen);

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

        // Weather forecast from stored data
        WeatherDTO weather = weatherService.getStoredWeather(listing);
        model.addAttribute("weather", weather);

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
                                @RequestParam(required = false) String venueName,
                                @RequestParam(required = false) String address,
                                @RequestParam(required = false) Double latitude,
                                @RequestParam(required = false) Double longitude,
                                RedirectAttributes redirectAttributes) {
        try {
            LocalDateTime dateTime = (scheduledDate != null && !scheduledDate.isBlank())
                    ? LocalDateTime.parse(scheduledDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
            SkillLevel skill = (skillLevel != null && !skillLevel.isBlank())
                    ? SkillLevel.valueOf(skillLevel.toUpperCase()) : null;
            PrivacySetting privacy = (privacySetting != null && !privacySetting.isBlank())
                    ? PrivacySetting.valueOf(privacySetting.toUpperCase()) : null;

            // Validate coordinates if provided
            if (latitude != null && longitude != null) {
                if (latitude < -90 || latitude > 90) {
                    throw new IllegalArgumentException("Latitude must be between -90 and 90.");
                }
                if (longitude < -180 || longitude > 180) {
                    throw new IllegalArgumentException("Longitude must be between -180 and 180.");
                }
            }

            gameListingService.updateListing(id, currentUser.getUserId(), dateTime, location, skill, privacy,
                    venueName, address, latitude, longitude);
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

    // ===== Additional Invitations: Show invite form for OPEN listings =====

    @GetMapping("/listings/{id}/invite")
    public String showInviteForm(@PathVariable Long id,
                                 @AuthenticationPrincipal CustomUserDetails currentUser,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        GameListing listing = gameListingService.getListingWithDetails(id);

        // Only creator can invite
        if (!listing.getCreator().getUserId().equals(currentUser.getUserId())) {
            redirectAttributes.addFlashAttribute("error", "Only the listing creator can invite players.");
            return "redirect:/listings/" + id;
        }

        // Listing must be OPEN
        if (!invitationService.isListingOpen(listing)) {
            redirectAttributes.addFlashAttribute("error", "Invitations can only be sent for OPEN listings.");
            return "redirect:/listings/" + id;
        }

        // Get friends/followers who can be invited
        List<Long> friendIds = followService.getFollowingIds(currentUser.getUserId());
        List<Long> alreadyInvitedIds = invitationService.getAlreadyInvitedUserIds(id);

        // Filter out already invited, already participating, and the creator
        List<com.gameon.model.entity.User> availableFriends = friendIds.stream()
                .filter(fid -> !alreadyInvitedIds.contains(fid))
                .filter(fid -> !gameJoinerRepository_isParticipating(fid, id))
                .map(fid -> userService.getUserById(fid))
                .filter(u -> u != null)
                .toList();

        model.addAttribute("listing", listing);
        model.addAttribute("friends", availableFriends);
        model.addAttribute("invitationHistory", invitationService.getInvitationHistory(id));
        return "listings/invite";
    }

    @PostMapping("/listings/{id}/invite")
    public String processInvitations(@PathVariable Long id,
                                     @AuthenticationPrincipal CustomUserDetails currentUser,
                                     @RequestParam(required = false) List<Long> invitedFriendIds,
                                     RedirectAttributes redirectAttributes) {
        if (invitedFriendIds == null || invitedFriendIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select at least one friend to invite.");
            return "redirect:/listings/" + id + "/invite";
        }

        try {
            int sentCount = invitationService.sendInvitations(id, currentUser.getUserId(), invitedFriendIds);
            if (sentCount > 0) {
                redirectAttributes.addFlashAttribute("success",
                        sentCount + " invitation" + (sentCount > 1 ? "s" : "") + " sent successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error",
                        "No invitations sent. Selected users may have already been invited or are participating.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/listings/" + id;
    }

    /**
     * Helper to check if a user is already participating in a listing.
     */
    private boolean gameJoinerRepository_isParticipating(Long userId, Long listingId) {
        JoinerStatus status = gameJoinerService.getUserJoinRequestStatus(userId, listingId);
        return status != null; // PENDING, ACCEPTED, or LOCKED
    }

    // ===== Map View: Get all active listings with coordinates as JSON =====

    @GetMapping("/listings/map-data")
    @ResponseBody
    public List<java.util.Map<String, Object>> getMapData(@AuthenticationPrincipal CustomUserDetails currentUser) {
        List<GameListing> listings = gameListingService.getActiveListingsWithCoordinates();
        return listings.stream().map(listing -> {
            java.util.Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("id", listing.getGameListingId());
            item.put("sport", listing.getFormat().getSport().getSportName());
            item.put("format", listing.getFormat().getFormatName());
            item.put("skillLevel", listing.getSkillLevel().name());
            item.put("scheduledDate", listing.getScheduledDate().format(
                    java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")));
            item.put("venueName", listing.getVenueName() != null ? listing.getVenueName() : listing.getLocation());
            item.put("latitude", listing.getLatitude());
            item.put("longitude", listing.getLongitude());
            item.put("creator", listing.getCreator().getUsername());
            return item;
        }).toList();
    }

    // ===== Nearby Listings: Get listings within a radius as JSON =====

    @GetMapping("/listings/nearby")
    @ResponseBody
    public List<java.util.Map<String, Object>> getNearbyListings(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10") double radius) {
        // Validate coordinates
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            return java.util.Collections.emptyList();
        }
        // Cap radius to 50 km
        if (radius > 50) radius = 50;
        if (radius < 1) radius = 1;

        List<GameListingService.NearbyListingResult> results =
                gameListingService.getNearbyListings(lat, lng, radius);

        return results.stream().map(r -> {
            GameListing listing = r.getListing();
            java.util.Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("id", listing.getGameListingId());
            item.put("sport", listing.getFormat().getSport().getSportName());
            item.put("format", listing.getFormat().getFormatName());
            item.put("skillLevel", listing.getSkillLevel().name());
            item.put("scheduledDate", listing.getScheduledDate().format(
                    java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")));
            item.put("venueName", listing.getVenueName() != null ? listing.getVenueName() : listing.getLocation());
            item.put("latitude", listing.getLatitude());
            item.put("longitude", listing.getLongitude());
            item.put("creator", listing.getCreator().getUsername());
            item.put("distanceKm", Math.round(r.getDistanceKm() * 10.0) / 10.0);
            return item;
        }).toList();
    }
}
