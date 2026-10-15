package com.gameon.controller;

import com.gameon.exception.BusinessRuleException;
import com.gameon.model.entity.Report;
import com.gameon.model.entity.User;
import com.gameon.model.enums.UserRole;
import com.gameon.security.CustomUserDetails;
import com.gameon.service.ReportService;
import com.gameon.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ReportController} Report &amp; Block (user) submission, following the
 * project's Mockito-only controller convention (direct method invocation).
 */
@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock ReportService reportService;
    @Mock UserService userService;
    @InjectMocks ReportController controller;

    private CustomUserDetails reporter;

    @BeforeEach
    void setUp() {
        User alice = new User("Alice", "password", UserRole.USER);
        alice.setUserId(1L);
        reporter = new CustomUserDetails(alice);
    }

    @Test
    void reportAndBlockCallsCombinedServiceRedirectsToSocialWithConfirmation() {
        when(reportService.reportAndBlockUser(eq(1L), eq(2L), eq("Harassment or bullying"), any()))
                .thenReturn(new Report());
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller.submitUserReport(2L, "Harassment or bullying", "details",
                reporter, redirect);

        // Combined, atomic report+block through the single existing blocking system.
        verify(reportService).reportAndBlockUser(1L, 2L, "Harassment or bullying", "details");
        // Redirects to the social hub, NOT back to the blocked user's profile.
        assertThat(view).isEqualTo("redirect:/social");
        assertThat(redirect.getFlashAttributes().get("success").toString())
                .isEqualTo("Report submitted and user blocked. You will no longer see each other's content.");
    }

    @Test
    void failureRedirectsBackToFormWithError() {
        when(reportService.reportAndBlockUser(anyLong(), anyLong(), any(), any()))
                .thenThrow(new BusinessRuleException("You cannot report your own account."));
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller.submitUserReport(2L, "Other", null, reporter, redirect);

        assertThat(view).isEqualTo("redirect:/report/user/2");
        assertThat(redirect.getFlashAttributes().get("error").toString())
                .contains("cannot report your own account");
    }
}
