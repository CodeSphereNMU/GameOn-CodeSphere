package com.gameon.controller;

import com.gameon.dto.auth.RegisterStep1Dto;
import com.gameon.dto.auth.RegisterStep2Dto;
import com.gameon.dto.auth.ResetPasswordDto;
import com.gameon.exception.ResourceNotFoundException;
import com.gameon.model.entity.Sport;
import com.gameon.model.enums.SkillLevel;
import com.gameon.security.CustomUserDetails;
import com.gameon.service.AuthService;
import com.gameon.repository.SportRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller handling authentication: login page display, registration (2-step), logout.
 * Login processing and logout are handled by Spring Security's form login mechanism.
 */
@Controller
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final SportRepository sportRepository;
    private final AuthenticationManager authenticationManager;
    private final com.gameon.repository.UserRepository userRepository;

    public AuthController(AuthService authService,
                          SportRepository sportRepository,
                          AuthenticationManager authenticationManager,
                          com.gameon.repository.UserRepository userRepository) {
        this.authService = authService;
        this.sportRepository = sportRepository;
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
    }

    // ===== LOGIN =====

    @GetMapping("/login")
    public String showLogin(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            @RequestParam(value = "expired", required = false) String expired,
                            @RequestParam(value = "invalid", required = false) String invalid,
                            @RequestParam(value = "banned", required = false) String banned,
                            @RequestParam(value = "suspended", required = false) String suspended,
                            @RequestParam(value = "u", required = false) String username,
                            Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully");
        }
        if (expired != null) {
            model.addAttribute("error", "Your session has expired. Please login again");
        }
        if (invalid != null) {
            model.addAttribute("error", "Your session is invalid. Please login again");
        }
        // Banned / suspended login experience: show the specific status message with the
        // stored reason, strikes and ban date, rather than a generic error.
        if (banned != null) {
            model.addAttribute("accountBanned", true);
            populateModerationDetails(username, model);
        } else if (suspended != null) {
            model.addAttribute("accountSuspended", true);
            populateModerationDetails(username, model);
        }
        return "auth/login";
    }

    /** Loads the ban/suspension detail (reason, strikes, date) for the login status message. */
    private void populateModerationDetails(String username, Model model) {
        if (username == null || username.isBlank()) {
            return;
        }
        userRepository.findByUsername(username).ifPresent(user -> {
            model.addAttribute("banReason", user.getBanReason());
            model.addAttribute("warningCount", user.getWarningCount());
            model.addAttribute("strikesForBan", com.gameon.model.entity.User.STRIKES_FOR_BAN);
            model.addAttribute("bannedAt", user.getBannedAt());
        });
    }

    // ===== FORGOT / RESET PASSWORD =====

    @GetMapping("/forgot-password")
    public String showForgotPassword(Model model) {
        if (!model.containsAttribute("resetPasswordDto")) {
            model.addAttribute("resetPasswordDto", new ResetPasswordDto());
        }
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@Valid @ModelAttribute("resetPasswordDto") ResetPasswordDto dto,
                                        BindingResult result,
                                        Model model) {
        // Server-side password match check (mirrors client-side validation)
        if (!dto.passwordsMatch()) {
            result.rejectValue("confirmPassword", "error.confirmPassword", "Passwords do not match");
        }

        // Do not allow reset for a username that does not exist
        if (dto.getUsername() != null && !authService.usernameExists(dto.getUsername())) {
            result.rejectValue("username", "error.username", "Username not found.");
        }

        if (result.hasErrors()) {
            return "auth/forgot-password";
        }

        try {
            authService.resetPassword(dto);
            model.addAttribute("resetSuccess", true);
            logger.info("Password reset completed via forgot-password page for user: {}", dto.getUsername());
            return "auth/forgot-password";
        } catch (ResourceNotFoundException e) {
            // Username disappeared between the check and the update (rare race)
            result.rejectValue("username", "error.username", "Username not found.");
            return "auth/forgot-password";
        } catch (Exception e) {
            // Never expose internal/database errors to the user
            logger.error("Password reset failed for user: {}", dto.getUsername(), e);
            model.addAttribute("error", "We couldn't reset your password. Please try again.");
            return "auth/forgot-password";
        }
    }

    // ===== REGISTRATION STEP 1 =====

    @GetMapping("/register")
    public String showRegister(Model model) {
        model.addAttribute("registerDto", new RegisterStep1Dto());
        return "auth/register";
    }

    @PostMapping("/register")
    public String processRegister(@Valid @ModelAttribute("registerDto") RegisterStep1Dto dto,
                                  BindingResult result,
                                  HttpServletRequest request,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        // Validate passwords match
        if (!dto.passwordsMatch()) {
            result.rejectValue("confirmPassword", "error.confirmPassword", "Passwords do not match");
        }

        // Validate username uniqueness
        if (!authService.isUsernameAvailable(dto.getUsername())) {
            result.rejectValue("username", "error.username", "Username already taken");
        }

        if (result.hasErrors()) {
            return "auth/register";
        }

        try {
            Long userId = authService.registerStep1(dto);
            // Store userId and raw password in session for step 2 auto-login
            request.getSession().setAttribute("registrationUserId", userId);
            request.getSession().setAttribute("registrationUsername", dto.getUsername());
            request.getSession().setAttribute("registrationPassword", dto.getPassword());
            return "redirect:/register-sports";
        } catch (Exception e) {
            logger.error("Registration failed for user: {}", dto.getUsername(), e);
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        }
    }

    // ===== REGISTRATION STEP 2 =====

    @GetMapping("/register-sports")
    public String showRegisterSports(HttpServletRequest request, Model model) {
        Long userId = (Long) request.getSession().getAttribute("registrationUserId");
        if (userId == null) {
            return "redirect:/register";
        }

        List<Sport> sports = sportRepository.findAll();
        model.addAttribute("sports", sports);
        model.addAttribute("skillLevels", SkillLevel.values());
        model.addAttribute("registerStep2Dto", new RegisterStep2Dto());
        return "auth/register-sports";
    }

    @PostMapping("/register-sports")
    public String processRegisterSports(@ModelAttribute("registerStep2Dto") RegisterStep2Dto dto,
                                        BindingResult result,
                                        HttpServletRequest request,
                                        HttpServletResponse response,
                                        Model model,
                                        RedirectAttributes redirectAttributes) {
        Long userId = (Long) request.getSession().getAttribute("registrationUserId");
        String username = (String) request.getSession().getAttribute("registrationUsername");
        String rawPassword = (String) request.getSession().getAttribute("registrationPassword");

        if (userId == null || username == null) {
            return "redirect:/register";
        }

        // Check if any sport was actually selected (sportId != null means checkbox was checked)
        boolean hasSelectedSport = dto.getSportSelections() != null &&
                dto.getSportSelections().stream().anyMatch(s -> s.getSportId() != null);

        if (!hasSelectedSport) {
            model.addAttribute("error", "Please select at least one sport");
            model.addAttribute("sports", sportRepository.findAll());
            model.addAttribute("skillLevels", SkillLevel.values());
            return "auth/register-sports";
        }

        try {
            authService.registerStep2(userId, dto);

            // Auto-login after successful registration
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(username, rawPassword);
            Authentication authentication = authenticationManager.authenticate(authToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Persist security context to session
            request.getSession().setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext());

            // Clean up registration session attributes
            request.getSession().removeAttribute("registrationUserId");
            request.getSession().removeAttribute("registrationUsername");
            request.getSession().removeAttribute("registrationPassword");

            redirectAttributes.addFlashAttribute("success", "Welcome to GameOn! Your account has been created.");
            return "redirect:/listings";
        } catch (Exception e) {
            logger.error("Registration step 2 failed for user ID: {}", userId, e);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("sports", sportRepository.findAll());
            model.addAttribute("skillLevels", SkillLevel.values());
            return "auth/register-sports";
        }
    }
}
