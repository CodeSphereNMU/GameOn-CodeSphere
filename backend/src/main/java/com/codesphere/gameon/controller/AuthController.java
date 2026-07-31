package com.codesphere.gameon.controller;

import com.codesphere.gameon.dao.UserDao;
import com.codesphere.gameon.dto.ApiResponse;
import com.codesphere.gameon.dto.LoginRequest;
import com.codesphere.gameon.exception.ApiException;
import com.codesphere.gameon.model.User;
import com.codesphere.gameon.service.AuthService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Handles authentication routes: login and session info.
 */
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final UserDao userDao;

    public AuthController(AuthService authService, UserDao userDao) {
        this.authService = authService;
        this.userDao = userDao;
    }

    public void register(Javalin app) {
        app.post("/api/auth/login", this::login);
        app.get("/api/auth/me", this::me);
    }

    /**
     * POST /api/auth/login
     * Authenticates user and creates a server-side session.
     */
    private void login(Context ctx) {
        LoginRequest request = ctx.bodyAsClass(LoginRequest.class);

        User user = authService.login(request.getUsername(), request.getPassword());

        // Create session after successful authentication
        ctx.sessionAttribute("userId", user.getUserId());

        ctx.json(ApiResponse.success(Map.of(
                "userId", user.getUserId(),
                "username", user.getUsername(),
                "typeOfUser", user.getTypeOfUser() != null ? user.getTypeOfUser() : "player"
        )));
    }

    /**
     * GET /api/auth/me
     * Returns the currently authenticated user's info from the session.
     */
    private void me(Context ctx) {
        Long userId = ctx.sessionAttribute("userId");

        if (userId == null) {
            throw ApiException.unauthorized("Login required");
        }

        User user = userDao.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("Login required"));

        ctx.json(ApiResponse.success(Map.of(
                "userId", user.getUserId(),
                "username", user.getUsername(),
                "typeOfUser", user.getTypeOfUser() != null ? user.getTypeOfUser() : "player"
        )));
    }
}
