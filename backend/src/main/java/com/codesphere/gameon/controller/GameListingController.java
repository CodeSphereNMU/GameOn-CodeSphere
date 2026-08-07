package com.codesphere.gameon.controller;

import com.codesphere.gameon.dto.ApiResponse;
import com.codesphere.gameon.dto.BrowseFilter;
import com.codesphere.gameon.dto.BrowseListingDto;
import com.codesphere.gameon.dto.CreateListingRequest;
import com.codesphere.gameon.dto.CreateListingResponse;
import com.codesphere.gameon.dto.ListingDetailDto;
import com.codesphere.gameon.dto.PaginatedResponse;
import com.codesphere.gameon.exception.ApiException;
import com.codesphere.gameon.service.BrowseListingService;
import com.codesphere.gameon.service.GameListingService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Handles game listing routes.
 */
public class GameListingController {

    private final GameListingService gameListingService;
    private final BrowseListingService browseListingService;

    public GameListingController(GameListingService gameListingService, BrowseListingService browseListingService) {
        this.gameListingService = gameListingService;
        this.browseListingService = browseListingService;
    }

    public void register(Javalin app) {
        app.post("/api/game-listings", this::createListing);
        app.get("/api/game-listings", this::browseListings);
        app.get("/api/game-listings/{id}", this::getListingDetail);
    }

    /**
     * POST /api/game-listings
     * Creates a new game listing for the authenticated user.
     */
    private void createListing(Context ctx) {
        Long userId = ctx.sessionAttribute("userId");
        if (userId == null) {
            throw ApiException.unauthorized("Login required");
        }

        CreateListingRequest request = ctx.bodyAsClass(CreateListingRequest.class);
        CreateListingResponse response = gameListingService.createListing(userId, request);

        ctx.status(HttpStatus.CREATED);
        ctx.json(ApiResponse.success(response));
    }

    /**
     * GET /api/game-listings
     * Returns a paginated list of browsable game listings for the authenticated user.
     * Supports optional filters: sportId, skillLevel, date, hideFull.
     */
    private void browseListings(Context ctx) {
        Long userId = ctx.sessionAttribute("userId");
        if (userId == null) {
            throw ApiException.unauthorized("Login required");
        }

        // Parse pagination params with defaults
        int page = parsePositiveInt(ctx.queryParam("page"), "1", "Invalid pagination parameters");
        int size = parsePositiveInt(ctx.queryParam("size"), "20", "Invalid pagination parameters");

        // Parse optional sportId
        Long sportId = null;
        String sportIdParam = ctx.queryParam("sportId");
        if (sportIdParam != null && !sportIdParam.isBlank()) {
            try {
                sportId = Long.parseLong(sportIdParam);
            } catch (NumberFormatException e) {
                throw ApiException.badRequest("Invalid sportId parameter");
            }
        }

        // Parse optional skillLevel (pass as-is)
        String skillLevel = ctx.queryParam("skillLevel");

        // Parse optional date filter
        LocalDate date = null;
        String dateParam = ctx.queryParam("date");
        if (dateParam != null && !dateParam.isBlank()) {
            try {
                date = LocalDate.parse(dateParam);
            } catch (DateTimeParseException e) {
                throw ApiException.badRequest("Invalid date format. Use ISO format: YYYY-MM-DD");
            }
        }

        // Parse hideFull (default false)
        String hideFullParam = ctx.queryParam("hideFull");
        boolean hideFull = Boolean.parseBoolean(hideFullParam != null ? hideFullParam : "false");

        // Build filter
        BrowseFilter filter = new BrowseFilter();
        filter.setPage(page);
        filter.setSize(size);
        filter.setSportId(sportId);
        filter.setSkillLevel(skillLevel);
        filter.setDate(date);
        filter.setHideFull(hideFull);

        // Call service
        PaginatedResponse<BrowseListingDto> paginatedResponse = browseListingService.browseListings(userId, filter);

        ctx.json(ApiResponse.success(paginatedResponse));
    }

    /**
     * GET /api/game-listings/{id}
     * Returns full detail for a single listing, including roster.
     * Access control is enforced by the service layer.
     */
    private void getListingDetail(Context ctx) {
        Long userId = ctx.sessionAttribute("userId");
        if (userId == null) {
            throw ApiException.unauthorized("Login required");
        }

        long listingId;
        try {
            listingId = Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("Invalid listing ID");
        }

        ListingDetailDto detailDto = browseListingService.getListingDetail(userId, listingId);
        ctx.json(ApiResponse.success(detailDto));
    }

    /**
     * Parses a query parameter as a positive integer.
     * Returns the parsed value, or the default if the param is null/blank.
     * Throws ApiException.badRequest if non-numeric or less than 1.
     */
    private int parsePositiveInt(String param, String defaultValue, String errorMessage) {
        String value = (param != null && !param.isBlank()) ? param : defaultValue;
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 1) {
                throw ApiException.badRequest(errorMessage);
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw ApiException.badRequest(errorMessage);
        }
    }
}
