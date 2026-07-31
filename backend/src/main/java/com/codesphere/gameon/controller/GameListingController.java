package com.codesphere.gameon.controller;

import com.codesphere.gameon.dto.ApiResponse;
import com.codesphere.gameon.dto.CreateListingRequest;
import com.codesphere.gameon.dto.CreateListingResponse;
import com.codesphere.gameon.exception.ApiException;
import com.codesphere.gameon.service.GameListingService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

/**
 * Handles game listing routes.
 */
public class GameListingController {

    private final GameListingService gameListingService;

    public GameListingController(GameListingService gameListingService) {
        this.gameListingService = gameListingService;
    }

    public void register(Javalin app) {
        app.post("/api/game-listings", this::createListing);
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
}
