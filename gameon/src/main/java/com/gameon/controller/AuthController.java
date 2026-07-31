package com.gameon.controller;

import com.gameon.dto.auth.RegisterStep1Dto;
import com.gameon.dto.auth.RegisterStep2Dto;
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

    public AuthController(AuthService authService,
                          SportRepository sportRepository,
                          AuthenticationManager authenticationManager) {
        this.authService = authService;
        this.sportRepository = sportRepository;
        this.authenticationManager = authenticationManager;
    }

    // ===== LOGIN =====

    @GetMapping("/login")
    public String showLogin(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            @RequestParam(value = "expired", required = false) String expired,
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
        return "auth/login";
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
    public String processRegisterSports(@Valid @ModelAttribute("registerStep2Dto") RegisterStep2Dto dto,
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

        if (dto.getSportSelections() == null || dto.getSportSelections().isEmpty()) {
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
