package com.gameon.config;

import com.gameon.security.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * Spring Security 6 Configuration for GameOn.
 * Implements form-based authentication with role-based access control.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF Protection - enabled with cookie repository for Thymeleaf compatibility
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            )

            // Authorization Rules
            .authorizeHttpRequests(auth -> auth
                // Public resources
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico").permitAll()

                // Authentication pages - accessible to anonymous users only
                .requestMatchers("/login", "/register", "/register-sports").permitAll()

                // Actuator health endpoint
                .requestMatchers("/actuator/health").permitAll()

                // Moderator-only endpoints
                .requestMatchers("/moderator/**").hasRole("MODERATOR")

                // Admin-only endpoints
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // All other requests require authentication
                .anyRequest().authenticated()
            )

            // Form-based Login
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(authenticationSuccessHandler())
                .failureUrl("/login?error=true")
                .permitAll()
            )

            // Logout Configuration
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )

            // Session Management
            .sessionManagement(session -> session
                .invalidSessionUrl("/login?invalid=true")
                .sessionFixation(fix -> fix.migrateSession())
                .sessionConcurrency(concurrency -> concurrency
                    .maximumSessions(1)
                    .expiredUrl("/login?expired=true")
                )
            )

            // Exception Handling
            .exceptionHandling(exceptions -> exceptions
                .accessDeniedPage("/error/403")
            )

            // Security Headers
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
                .xssProtection(xss -> xss.disable())
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'; " +
                                     "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; " +
                                     "font-src 'self' https://cdn.jsdelivr.net;")
                )
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Custom success handler for role-based redirect after login.
     * Moderators go to /moderator/reports, regular users go to /listings.
     */
    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            boolean isModerator = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_MODERATOR"));
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

            if (isAdmin) {
                response.sendRedirect("/admin/dashboard");
            } else if (isModerator) {
                response.sendRedirect("/moderator/reports");
            } else {
                response.sendRedirect("/listings");
            }
        };
    }
}
