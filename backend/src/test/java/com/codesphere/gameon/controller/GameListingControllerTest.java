package com.codesphere.gameon.controller;

import com.codesphere.gameon.dto.*;
import com.codesphere.gameon.exception.ApiException;
import com.codesphere.gameon.service.BrowseListingService;
import com.codesphere.gameon.service.GameListingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GameListingController browse/detail handlers.
 * Spins up a real Javalin instance on a random port and uses HttpClient for requests.
 */
class GameListingControllerTest {

    private Javalin app;
    private int port;
    private HttpClient httpClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        httpClient = HttpClient.newHttpClient();

        // Create stub services
        BrowseListingService stubBrowseService = new StubBrowseListingService();
        GameListingService stubGameListingService = new GameListingService(
                null, null, null, null, null, null, null, null, null);

        GameListingController controller = new GameListingController(stubGameListingService, stubBrowseService);

        app = Javalin.create();

        // Register exception handler (same as JavalinConfig does)
        app.exception(ApiException.class, (e, ctx) -> {
            ctx.status(e.getStatus());
            ctx.json(Map.of("success", false, "error", e.getMessage()));
        });

        // Simulate authentication via X-Test-User-Id header
        app.before("/api/*", ctx -> {
            String testUserId = ctx.header("X-Test-User-Id");
            if (testUserId != null) {
                ctx.sessionAttribute("userId", Long.parseLong(testUserId));
            }
        });

        // Register controller routes
        controller.register(app);

        app.start(0);
        port = app.port();
    }

    @AfterEach
    void tearDown() {
        if (app != null) {
            app.stop();
        }
    }

    // ========================================================
    // Authentication tests (401)
    // ========================================================

    @Test
    void shouldReturn401WhenBrowseRequestHasNoSession() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/game-listings"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode());
        JsonNode body = objectMapper.readTree(response.body());
        assertFalse(body.get("success").asBoolean());
        assertEquals("Login required", body.get("error").asText());
    }

    @Test
    void shouldReturn401WhenDetailRequestHasNoSession() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/game-listings/1"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode());
        JsonNode body = objectMapper.readTree(response.body());
        assertFalse(body.get("success").asBoolean());
        assertEquals("Login required", body.get("error").asText());
    }

    // ========================================================
    // Pagination validation tests (400)
    // ========================================================

    @Test
    void shouldReturn400WhenPageIsInvalid() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/game-listings?page=abc"))
                .header("X-Test-User-Id", "1")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
        JsonNode body = objectMapper.readTree(response.body());
        assertFalse(body.get("success").asBoolean());
        assertEquals("Invalid pagination parameters", body.get("error").asText());
    }

    @Test
    void shouldReturn400WhenPageIsZero() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/game-listings?page=0"))
                .header("X-Test-User-Id", "1")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
        JsonNode body = objectMapper.readTree(response.body());
        assertFalse(body.get("success").asBoolean());
        assertEquals("Invalid pagination parameters", body.get("error").asText());
    }

    @Test
    void shouldReturn400WhenSizeIsNegative() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/game-listings?size=-1"))
                .header("X-Test-User-Id", "1")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
        JsonNode body = objectMapper.readTree(response.body());
        assertFalse(body.get("success").asBoolean());
        assertEquals("Invalid pagination parameters", body.get("error").asText());
    }

    // ========================================================
    // Date format validation test (400)
    // ========================================================

    @Test
    void shouldReturn400WhenDateFormatIsInvalid() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/game-listings?date=not-a-date"))
                .header("X-Test-User-Id", "1")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
        JsonNode body = objectMapper.readTree(response.body());
        assertFalse(body.get("success").asBoolean());
        assertEquals("Invalid date format. Use ISO format: YYYY-MM-DD", body.get("error").asText());
    }

    // ========================================================
    // Success tests (200 with ApiResponse wrapper)
    // ========================================================

    @Test
    void shouldReturn200WithApiResponseWrapperForValidBrowseRequest() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/game-listings"))
                .header("X-Test-User-Id", "1")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        JsonNode body = objectMapper.readTree(response.body());
        assertTrue(body.get("success").asBoolean());
        assertNotNull(body.get("data"));

        // Verify paginated response structure
        JsonNode data = body.get("data");
        assertNotNull(data.get("items"));
        assertTrue(data.get("items").isArray());
        assertEquals(1, data.get("page").asInt());
        assertEquals(20, data.get("size").asInt());
        assertEquals(0, data.get("totalItems").asInt());
        assertEquals(0, data.get("totalPages").asInt());
    }

    @Test
    void shouldReturn200WithApiResponseWrapperForValidDetailRequest() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/game-listings/1"))
                .header("X-Test-User-Id", "1")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        JsonNode body = objectMapper.readTree(response.body());
        assertTrue(body.get("success").asBoolean());
        assertNotNull(body.get("data"));

        // Verify listing detail structure
        JsonNode data = body.get("data");
        assertEquals(1, data.get("gameListingId").asLong());
        assertEquals("Basketball", data.get("sportName").asText());
        assertEquals("3v3", data.get("formatName").asText());
    }

    // ========================================================
    // Stub BrowseListingService
    // ========================================================

    /**
     * Stub that overrides BrowseListingService methods to return fixed responses
     * without touching the database.
     */
    private static class StubBrowseListingService extends BrowseListingService {

        StubBrowseListingService() {
            super(null, null, null, null, null, null);
        }

        @Override
        public PaginatedResponse<BrowseListingDto> browseListings(long userId, BrowseFilter filter) {
            return new PaginatedResponse<>(
                    Collections.emptyList(),
                    filter.getPage(),
                    filter.getSize(),
                    0,
                    0
            );
        }

        @Override
        public ListingDetailDto getListingDetail(long userId, long listingId) {
            ListingDetailDto detail = new ListingDetailDto();
            detail.setGameListingId(listingId);
            detail.setSportName("Basketball");
            detail.setFormatName("3v3");
            detail.setSkillLevel("Intermediate");
            detail.setDate("2026-08-15");
            detail.setSessionWindow("14:00\u201315:00");
            detail.setLocation("University Fields");
            detail.setSpotsFilled(3);
            detail.setTotalSpots(6);
            detail.setCreatorUsername("test_user");
            detail.setHasPositions(false);
            detail.setPrivate(false);
            detail.setTeamA(List.of());
            detail.setTeamB(List.of());
            return detail;
        }
    }
}
