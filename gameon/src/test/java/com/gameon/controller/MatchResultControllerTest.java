package com.gameon.controller;

import com.gameon.model.entity.GameJoiner;
import com.gameon.model.entity.GameListing;
import com.gameon.model.entity.MatchResult;
import com.gameon.model.entity.SportFormat;
import com.gameon.model.entity.Sport;
import com.gameon.model.entity.User;
import com.gameon.model.enums.JoinerStatus;
import com.gameon.model.enums.PrivacySetting;
import com.gameon.model.enums.SkillLevel;
import com.gameon.model.enums.Team;
import com.gameon.model.enums.UserRole;
import com.gameon.service.GameListingService;
import com.gameon.service.MatchResultService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MatchResultController.viewResult (the Match Results view page).
 * Follows the same Mockito-only controller conventions as the rest of the suite
 * (no Spring context / MockMvc): the controller method is invoked directly and
 * the resulting view name and model attributes are asserted.
 */
@ExtendWith(MockitoExtension.class)
class MatchResultControllerTest {

    @Mock MatchResultService matchResultService;
    @Mock GameListingService gameListingService;
    @InjectMocks MatchResultController controller;

    private User creator;      // Team A (always)
    private User teamAMate;    // extra Team A player
    private User teamBPlayer;  // Team B player
    private GameListing listing;

    @BeforeEach
    void setUp() {
        creator = user(1L, "Creator");
        teamAMate = user(2L, "AlphaMate");
        teamBPlayer = user(3L, "BravoPlayer");

        Sport sport = new Sport("Football");
        sport.setSportId(5L);
        SportFormat format = new SportFormat("5-a-side", 10, false, 60, sport);
        format.setFormatId(6L);

        listing = new GameListing(creator, format, SkillLevel.INTERMEDIATE,
                LocalDateTime.now().minusHours(2), "Pitch", PrivacySetting.PUBLIC, 60);
        listing.setGameListingId(15L);

        // Confirmed/locked roster: creator (A), one more A, one B.
        GameJoiner creatorJoiner = joiner(creator, Team.A, JoinerStatus.LOCKED);
        GameJoiner aMate = joiner(teamAMate, Team.A, JoinerStatus.LOCKED);
        GameJoiner bPlayer = joiner(teamBPlayer, Team.B, JoinerStatus.LOCKED);
        listing.setJoiners(List.of(creatorJoiner, aMate, bPlayer));
    }

    @Test
    @DisplayName("viewResult returns the view page with result and listing in the model")
    void viewResultReturnsViewWithModel() {
        MatchResult result = new MatchResult(listing, 3, 0);
        when(matchResultService.getResultForListing(15L)).thenReturn(result);
        when(gameListingService.getListingDetail(15L)).thenReturn(listing);

        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller.viewResult(15L, model, redirect);

        assertThat(view).isEqualTo("match-result/view");
        assertThat(model.getAttribute("result")).isSameAs(result);
        assertThat(model.getAttribute("listing")).isSameAs(listing);
    }

    @Test
    @DisplayName("Any authenticated user (non-creator) can view a recorded result")
    void nonCreatorCanViewResult() {
        // Controller takes no principal and performs no creator check for viewing.
        MatchResult result = new MatchResult(listing, 3, 0);
        when(matchResultService.getResultForListing(15L)).thenReturn(result);
        when(gameListingService.getListingDetail(15L)).thenReturn(listing);

        String view = controller.viewResult(15L, new ExtendedModelMap(), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("match-result/view");
        verify(matchResultService).getResultForListing(15L);
        verify(gameListingService).getListingDetail(15L);
    }

    @Test
    @DisplayName("viewResult redirects to history when no result exists")
    void viewResultRedirectsWhenNoResult() {
        when(matchResultService.getResultForListing(99L)).thenReturn(null);
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String view = controller.viewResult(99L, new ExtendedModelMap(), redirect);

        assertThat(view).isEqualTo("redirect:/lobby/history");
        assertThat(redirect.getFlashAttributes().get("error").toString())
                .contains("No match result found");
        verify(gameListingService, never()).getListingDetail(anyLong());
    }

    @Test
    @DisplayName("Listing passed to the view keeps the creator on Team A with the confirmed roster")
    void listingModelCarriesCorrectTeamAssignments() {
        MatchResult result = new MatchResult(listing, 3, 0);
        when(matchResultService.getResultForListing(15L)).thenReturn(result);
        when(gameListingService.getListingDetail(15L)).thenReturn(listing);

        Model model = new ExtendedModelMap();
        controller.viewResult(15L, model, new RedirectAttributesModelMap());

        GameListing modelListing = (GameListing) model.getAttribute("listing");
        assertThat(modelListing).isNotNull();

        // Creator is always Team A.
        GameJoiner creatorJoiner = modelListing.getJoiners().stream()
                .filter(j -> j.getUser().getUserId().equals(creator.getUserId()))
                .findFirst().orElseThrow();
        assertThat(creatorJoiner.getTeam()).isEqualTo(Team.A);

        // Team A roster = creator + one mate; Team B = one player. No duplicate creator.
        List<String> teamA = modelListing.getJoiners().stream()
                .filter(j -> j.getTeam() == Team.A)
                .map(j -> j.getUser().getUsername())
                .toList();
        List<String> teamB = modelListing.getJoiners().stream()
                .filter(j -> j.getTeam() == Team.B)
                .map(j -> j.getUser().getUsername())
                .toList();

        assertThat(teamA).containsExactlyInAnyOrder("Creator", "AlphaMate");
        assertThat(teamA).filteredOn(name -> name.equals("Creator")).hasSize(1);
        assertThat(teamB).containsExactly("BravoPlayer");
    }

    @Test
    @DisplayName("viewResult loads the listing via the eager-fetch detail query so the roster is accessible")
    void viewResultUsesDetailQueryAndRosterIsAccessible() {
        // getListingDetail() -> findDetailById() eager-fetches joiners and their users,
        // which is what prevents the LazyInitializationException during template rendering.
        MatchResult result = new MatchResult(listing, 3, 0);
        when(matchResultService.getResultForListing(15L)).thenReturn(result);
        when(gameListingService.getListingDetail(15L)).thenReturn(listing);

        Model model = new ExtendedModelMap();
        controller.viewResult(15L, model, new RedirectAttributesModelMap());

        // The controller must NOT use the roster-less query for this view.
        verify(gameListingService).getListingDetail(15L);
        verify(gameListingService, never()).getListingWithDetails(anyLong());

        // Simulate the template traversal: reading joiners + username + team must not throw.
        GameListing modelListing = (GameListing) model.getAttribute("listing");
        assertThat(modelListing.getJoiners()).isNotEmpty();
        modelListing.getJoiners().forEach(j -> {
            assertThat(j.getUser().getUsername()).isNotBlank();
            assertThat(j.getTeam()).isNotNull();
        });
    }

    @Test
    @DisplayName("getWinners computes the display outcome from the final score")
    void winnerDerivedFromScore() {
        assertThat(new MatchResult(listing, 3, 0).getWinners()).isEqualTo("TEAM_A");
        assertThat(new MatchResult(listing, 1, 2).getWinners()).isEqualTo("TEAM_B");
        assertThat(new MatchResult(listing, 2, 2).getWinners()).isEqualTo("DRAW");
    }

    // ===== Helpers =====

    private GameJoiner joiner(User user, Team team, JoinerStatus status) {
        GameJoiner joiner = new GameJoiner(user, listing, team);
        joiner.setStatus(status);
        return joiner;
    }

    private User user(Long id, String name) {
        User user = new User(name, "password", UserRole.USER);
        user.setUserId(id);
        return user;
    }
}
