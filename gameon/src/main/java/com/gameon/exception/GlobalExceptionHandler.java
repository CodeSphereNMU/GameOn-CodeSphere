package com.gameon.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Global exception handler for the GameOn application.
 * Catches all unhandled exceptions and renders appropriate error pages.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles resource not found exceptions (custom 404).
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ModelAndView handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        logger.warn("Resource not found: {} | URL: {}", ex.getMessage(), request.getRequestURI());
        ModelAndView mav = new ModelAndView("error/404");
        mav.addObject("message", ex.getMessage());
        mav.setStatus(HttpStatus.NOT_FOUND);
        return mav;
    }

    /**
     * Handles business rule violations.
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ModelAndView handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {
        logger.warn("Business rule violation: {} | URL: {}", ex.getMessage(), request.getRequestURI());
        ModelAndView mav = new ModelAndView("error/400");
        mav.addObject("message", ex.getMessage());
        mav.addObject("ruleCode", ex.getRuleCode());
        mav.setStatus(HttpStatus.BAD_REQUEST);
        return mav;
    }

    /**
     * Handles duplicate resource exceptions.
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ModelAndView handleDuplicate(DuplicateResourceException ex, HttpServletRequest request) {
        logger.warn("Duplicate resource: {} | URL: {}", ex.getMessage(), request.getRequestURI());
        ModelAndView mav = new ModelAndView("error/409");
        mav.addObject("message", ex.getMessage());
        mav.setStatus(HttpStatus.CONFLICT);
        return mav;
    }

    /**
     * Handles unauthorized access exceptions.
     */
    @ExceptionHandler(UnauthorizedAccessException.class)
    public ModelAndView handleUnauthorized(UnauthorizedAccessException ex, HttpServletRequest request) {
        logger.warn("Unauthorized access: {} | URL: {} | User: {}",
                ex.getMessage(), request.getRequestURI(), request.getRemoteUser());
        ModelAndView mav = new ModelAndView("error/403");
        mav.addObject("message", ex.getMessage());
        mav.setStatus(HttpStatus.FORBIDDEN);
        return mav;
    }

    /**
     * Handles Spring Security access denied exceptions.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        logger.warn("Access denied: {} | URL: {} | User: {}",
                ex.getMessage(), request.getRequestURI(), request.getRemoteUser());
        ModelAndView mav = new ModelAndView("error/403");
        mav.addObject("message", "You do not have permission to access this page");
        mav.setStatus(HttpStatus.FORBIDDEN);
        return mav;
    }

    /**
     * Handles bad credentials (authentication failure).
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ModelAndView handleBadCredentials(BadCredentialsException ex) {
        logger.warn("Authentication failed: bad credentials");
        ModelAndView mav = new ModelAndView("auth/login");
        mav.addObject("error", "Invalid username or password");
        return mav;
    }

    /**
     * Handles validation errors from @Valid annotations.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ModelAndView handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        logger.warn("Validation error on URL: {}", request.getRequestURI());
        ModelAndView mav = new ModelAndView("error/400");
        mav.addObject("message", "Validation failed. Please check your input.");
        mav.addObject("errors", ex.getBindingResult().getAllErrors());
        mav.setStatus(HttpStatus.BAD_REQUEST);
        return mav;
    }

    /**
     * Handles Spring's no resource found (static resources 404).
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ModelAndView handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        logger.debug("Static resource not found: {}", request.getRequestURI());
        ModelAndView mav = new ModelAndView("error/404");
        mav.addObject("message", "The page you're looking for doesn't exist");
        mav.setStatus(HttpStatus.NOT_FOUND);
        return mav;
    }

    /**
     * Handles database constraint violations (unique key, FK violations, etc.).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ModelAndView handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        logger.error("Data integrity violation on URL: {} | Cause: {}",
                request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        ModelAndView mav = new ModelAndView("error/409");
        mav.addObject("message", "A data conflict occurred. The record may already exist or a required reference is missing.");
        mav.setStatus(HttpStatus.CONFLICT);
        return mav;
    }

    /**
     * Handles unsupported HTTP methods (e.g., GET on a POST-only endpoint).
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ModelAndView handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        logger.warn("Method not supported: {} {} | Supported: {}",
                ex.getMethod(), request.getRequestURI(), ex.getSupportedMethods());
        ModelAndView mav = new ModelAndView("error/400");
        mav.addObject("message", "This action is not supported. Please use the navigation to access features.");
        mav.setStatus(HttpStatus.METHOD_NOT_ALLOWED);
        return mav;
    }

    /**
     * Handles missing required request parameters.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ModelAndView handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        logger.warn("Missing request parameter: {} | URL: {}", ex.getParameterName(), request.getRequestURI());
        ModelAndView mav = new ModelAndView("error/400");
        mav.addObject("message", String.format("Required parameter '%s' is missing", ex.getParameterName()));
        mav.setStatus(HttpStatus.BAD_REQUEST);
        return mav;
    }

    /**
     * Handles type mismatch in request parameters (e.g., string where number expected).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ModelAndView handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        logger.warn("Type mismatch for parameter '{}' on URL: {} | Value: {}",
                ex.getName(), request.getRequestURI(), ex.getValue());
        ModelAndView mav = new ModelAndView("error/400");
        mav.addObject("message", String.format("Invalid value for '%s'. Please check your input.", ex.getName()));
        mav.setStatus(HttpStatus.BAD_REQUEST);
        return mav;
    }

    /**
     * Handles all other unhandled exceptions (500 internal server error).
     */
    @ExceptionHandler(Exception.class)
    public ModelAndView handleGenericException(Exception ex, HttpServletRequest request) {
        logger.error("Unhandled exception on URL: {} | Type: {} | Message: {}",
                request.getRequestURI(), ex.getClass().getSimpleName(), ex.getMessage(), ex);
        ModelAndView mav = new ModelAndView("error/500");
        mav.addObject("message", "An unexpected error occurred. Please try again later.");
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        return mav;
    }
}
