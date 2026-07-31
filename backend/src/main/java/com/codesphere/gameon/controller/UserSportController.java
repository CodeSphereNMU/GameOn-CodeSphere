package com.codesphere.gameon.controller;

import com.codesphere.gameon.dao.SportDao;
import com.codesphere.gameon.dto.ApiResponse;
import com.codesphere.gameon.dto.SportDto;
import com.codesphere.gameon.exception.ApiException;
import com.codesphere.gameon.model.Sport;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles routes for the authenticated user's sport profile.
 */
public class UserSportController {

    private final SportDao sportDao;

    public UserSportController(SportDao sportDao) {
        this.sportDao = sportDao;
    }

    public void register(Javalin app) {
        app.get("/api/users/me/sports", this::getMySports);
    }

    /**
     * GET /api/users/me/sports
     * Returns sports registered on the authenticated user's profile.
     */
    private void getMySports(Context ctx) {
        Long userId = ctx.sessionAttribute("userId");
        if (userId == null) {
            throw ApiException.unauthorized("Login required");
        }

        List<Sport> sports = sportDao.findSportsByUserId(userId);
        List<SportDto> dtos = sports.stream()
                .map(s -> {
                    String skillLevel = sportDao.getUserSkillLevel(userId, s.getSportId());
                    return new SportDto(s.getSportId(), s.getSportName(), skillLevel);
                })
                .collect(Collectors.toList());

        ctx.json(ApiResponse.success(dtos));
    }
}
