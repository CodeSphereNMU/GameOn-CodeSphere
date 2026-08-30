package com.gameon.controller;

import com.gameon.security.CustomUserDetails;
import com.gameon.service.GameJoinerService;
import com.gameon.service.GameListingService;
import com.gameon.service.ListingLifecycleService;
import com.gameon.service.MatchResultService;
import com.gameon.service.SportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the LobbyController last-call endpoint.
 * Verifies parameter handling, service delegation, and error cases
 * using the same Mockito conventions as the rest of the test suite.
 */
@ExtendWith(MockitoExtension.class)
class LobbyControllerLastCallTest {

    @Mock GameListingService gameListingService;
    @Mock GameJoinerService gameJoinerService;
    @Mock MatchResultService matchResultService;
    @Mock SportService sportService;
    @Mock ListingLifecycleService listingLifecycleService;
    @InjectMocks LobbyController controller;

    private CustomUserDetails mockUser;
    private RedirectAttributesModelMap redirectAttributes;

    @BeforeEach
void setUp() {
    mockUser = mock(CustomUserDetails.class);
    redirectAttributes = new RedirectAttributesModelMap();
}

    @Test
@DisplayName("selectedUserIds posted correctly calls service with correct arguments")
void lastCallWithSelectedUserIdsCallsService() {
    when(mockUser.getUserId()).thenReturn(1L);

    String result = controller.approveLastCall(
            22L,
            List.of(5L, 7L),
            mockUser,
            redirectAttributes
    );

    assertThat(result).isEqualTo("redirect:/lobby/requests/22");
    verify(gameJoinerService)
            .approveLastCallRequesters(22L, 1L, List.of(5L, 7L));

    assertThat(redirectAttributes.getFlashAttributes().get("success").toString())
            .contains("2 player(s)");
}

    @Test
    @DisplayName("Null selectedUserIds redirects with error, does not call service")
    void lastCallWithNullSelectionRedirectsWithError() {
        String result = controller.approveLastCall(22L, null, mockUser, redirectAttributes);

        assertThat(result).isEqualTo("redirect:/lobby/requests/22");
        verify(gameJoinerService, never()).approveLastCallRequesters(anyLong(), anyLong(), anyList());
        assertThat(redirectAttributes.getFlashAttributes().get("error").toString())
                .contains("select at least one");
    }

    @Test
    @DisplayName("Empty selectedUserIds list redirects with error, does not call service")
    void lastCallWithEmptyListRedirectsWithError() {
        String result = controller.approveLastCall(22L, List.of(), mockUser, redirectAttributes);

        assertThat(result).isEqualTo("redirect:/lobby/requests/22");
        verify(gameJoinerService, never()).approveLastCallRequesters(anyLong(), anyLong(), anyList());
        assertThat(redirectAttributes.getFlashAttributes().get("error").toString())
                .contains("select at least one");
    }

    @Test
@DisplayName("Normal Accept endpoint does not invoke last-call service method")
void acceptRequestDoesNotTriggerLastCall() {
    when(mockUser.getUserId()).thenReturn(1L);

    controller.acceptRequest(22L, 5L, mockUser, redirectAttributes);

    verify(gameJoinerService).acceptRequest(22L, 5L, 1L);
    verify(gameJoinerService, never())
            .approveLastCallRequesters(anyLong(), anyLong(), anyList());
}

    @Test
@DisplayName("Normal Reject endpoint does not invoke last-call service method")
void rejectRequestDoesNotTriggerLastCall() {
    when(mockUser.getUserId()).thenReturn(1L);

    controller.rejectRequest(22L, 5L, mockUser, redirectAttributes);

    verify(gameJoinerService).rejectRequest(22L, 5L, 1L);
    verify(gameJoinerService, never())
            .approveLastCallRequesters(anyLong(), anyLong(), anyList());
}
}
