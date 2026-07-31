package com.codesphere.gameon.service;

import com.codesphere.gameon.dao.UserDao;
import com.codesphere.gameon.exception.ApiException;
import com.codesphere.gameon.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Business logic for authentication (login).
 */
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final String INVALID_CREDENTIALS_MSG = "Invalid username or password";

    private final UserDao userDao;

    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * Authenticates a user by username and password (plain text comparison).
     *
     * @param username the submitted username
     * @param password the submitted password
     * @return the authenticated User
     * @throws ApiException with 400 if input is blank, or 401 if credentials are invalid
     */
    public User login(String username, String password) {
        if (username == null || username.isBlank()) {
            throw ApiException.badRequest("Username is required");
        }
        if (password == null || password.isBlank()) {
            throw ApiException.badRequest("Password is required");
        }

        Optional<User> userOpt = userDao.findByUsername(username.trim());

        if (userOpt.isEmpty()) {
            logger.debug("Login attempt for non-existent username");
            throw ApiException.unauthorized(INVALID_CREDENTIALS_MSG);
        }

        User user = userOpt.get();

        if (!user.getPassword().equals(password)) {
            logger.debug("Login attempt with incorrect password for user ID: {}", user.getUserId());
            throw ApiException.unauthorized(INVALID_CREDENTIALS_MSG);
        }

        logger.info("User logged in successfully: {}", user.getUsername());
        return user;
    }
}
