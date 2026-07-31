package com.codesphere.gameon.controller;

import com.codesphere.gameon.dao.PositionDao;
import com.codesphere.gameon.dao.SportFormatDao;
import com.codesphere.gameon.dto.ApiResponse;
import com.codesphere.gameon.dto.FormatDto;
import com.codesphere.gameon.dto.PositionDto;
import com.codesphere.gameon.exception.ApiException;
import com.codesphere.gameon.model.Position;
import com.codesphere.gameon.model.SportFormat;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles lookup routes for sport formats and positions.
 */
public class SportController {

    private final SportFormatDao sportFormatDao;
    private final PositionDao positionDao;

    public SportController(SportFormatDao sportFormatDao, PositionDao positionDao) {
        this.sportFormatDao = sportFormatDao;
        this.positionDao = positionDao;
    }

    public void register(Javalin app) {
        app.get("/api/sports/{sportId}/formats", this::getFormats);
        app.get("/api/formats/{formatId}/positions", this::getPositions);
    }

    /**
     * GET /api/sports/{sportId}/formats
     * Returns all formats for the given sport.
     */
    private void getFormats(Context ctx) {
        Long userId = ctx.sessionAttribute("userId");
        if (userId == null) {
            throw ApiException.unauthorized("Login required");
        }

        long sportId = parseLongParam(ctx, "sportId");
        List<SportFormat> formats = sportFormatDao.findFormatsBySportId(sportId);
        List<FormatDto> dtos = formats.stream()
                .map(f -> new FormatDto(f.getFormatId(), f.getFormatName(), f.isHasPositions(), f.getNoPlayers()))
                .collect(Collectors.toList());

        ctx.json(ApiResponse.success(dtos));
    }

    /**
     * GET /api/formats/{formatId}/positions
     * Returns all valid positions for a given format.
     */
    private void getPositions(Context ctx) {
        Long userId = ctx.sessionAttribute("userId");
        if (userId == null) {
            throw ApiException.unauthorized("Login required");
        }

        long formatId = parseLongParam(ctx, "formatId");
        List<Position> positions = positionDao.findPositionsByFormatId(formatId);
        List<PositionDto> dtos = positions.stream()
                .map(p -> new PositionDto(p.getPositionId(), p.getPositionName()))
                .collect(Collectors.toList());

        ctx.json(ApiResponse.success(dtos));
    }

    private long parseLongParam(Context ctx, String param) {
        try {
            return Long.parseLong(ctx.pathParam(param));
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("Invalid " + param + " parameter");
        }
    }
}
