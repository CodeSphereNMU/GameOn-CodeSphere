package com.codesphere.gameon.controller;

import com.codesphere.gameon.dao.FollowDao;
import com.codesphere.gameon.dto.ApiResponse;
import com.codesphere.gameon.dto.FriendDto;
import com.codesphere.gameon.exception.ApiException;
import com.codesphere.gameon.model.User;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles routes for mutual friends (mutual followers).
 */
public class FriendController {

    private final FollowDao followDao;

    public FriendController(FollowDao followDao) {
        this.followDao = followDao;
    }

    public void register(Javalin app) {
        app.get("/api/users/me/friends", this::getMyFriends);
    }

    /**
     * GET /api/users/me/friends
     * Returns all mutual followers (friends) for the authenticated user.
     */
    private void getMyFriends(Context ctx) {
        Long userId = ctx.sessionAttribute("userId");
        if (userId == null) {
            throw ApiException.unauthorized("Login required");
        }

        List<User> friends = followDao.findMutualFollowers(userId);
        List<FriendDto> dtos = friends.stream()
                .map(f -> new FriendDto(f.getUserId(), f.getUsername()))
                .collect(Collectors.toList());

        ctx.json(ApiResponse.success(dtos));
    }
}
