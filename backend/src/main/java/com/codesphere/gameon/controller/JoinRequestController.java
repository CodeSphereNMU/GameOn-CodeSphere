package com.codesphere.gameon.controller;

import com.codesphere.gameon.dto.ApiResponse;
import com.codesphere.gameon.dto.JoinRequestRequest;
import com.codesphere.gameon.dto.JoinRequestResponse;
import com.codesphere.gameon.exception.ApiException;
import com.codesphere.gameon.service.JoinRequestService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

/**
 * Handles join request routes.
 */
public class JoinRequestController {

    private final JoinRequestService joinRequestService;

    public JoinRequestController(JoinRequestService joinRequestService) {
        this.joinRequestService = joinRequestService;
    }

    public void register(Javalin app) {
        app.post("/api/game-listings/{id}/join-requests", this::createJoinRequest);
    }

    /**
     * POST /api/game-listings/{id}/join-requests
     * Creates a new join request for the specified game listing.
     */
    private void createJoinRequest(Context ctx) {
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

        JoinRequestRequest request = ctx.bodyAsClass(JoinRequestRequest.class);
        JoinRequestResponse response = joinRequestService.createJoinRequest(userId, listingId, request);

        ctx.status(HttpStatus.CREATED);
        ctx.json(ApiResponse.success(response));
    }
}
