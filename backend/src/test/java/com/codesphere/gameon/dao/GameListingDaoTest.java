package com.codesphere.gameon.dao;

import com.codesphere.gameon.dto.BrowseFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GameListingDao query building logic.
 * Verifies A500 (Hide Expired Listings): both the results query and the count query
 * include the GETDATE() condition that excludes listings whose scheduled date is in the past.
 *
 * These tests use reflection to invoke the private buildBrowseQuery method directly,
 * avoiding the need for a database connection. The actual SQL filtering was also
 * manually verified against SQL Server.
 */
class GameListingDaoTest {

    private GameListingDao dao;
    private Method buildBrowseQuery;

    @BeforeEach
    void setUp() throws Exception {
        // Instantiate with null DataSource — we only test query building, not execution
        dao = new GameListingDao(null);

        // Access the private buildBrowseQuery method via reflection
        buildBrowseQuery = GameListingDao.class.getDeclaredMethod(
                "buildBrowseQuery", List.class, BrowseFilter.class, List.class, boolean.class);
        buildBrowseQuery.setAccessible(true);
    }

    // ========================================================
    // A500 - Hide Expired Listings
    // ========================================================

    @Test
    void shouldExcludeExpiredListingsInResultsQuery() throws Exception {
        List<Long> sportIds = List.of(1L, 2L);
        BrowseFilter filter = new BrowseFilter();
        List<Object> params = new ArrayList<>();

        String sql = (String) buildBrowseQuery.invoke(dao, sportIds, filter, params, false);

        assertTrue(sql.contains("gl.[date] > GETDATE()"),
                "Results query must filter out expired listings using GETDATE()");
    }

    @Test
    void shouldExcludeExpiredListingsInCountQuery() throws Exception {
        List<Long> sportIds = List.of(1L, 2L);
        BrowseFilter filter = new BrowseFilter();
        List<Object> params = new ArrayList<>();

        String sql = (String) buildBrowseQuery.invoke(dao, sportIds, filter, params, true);

        assertTrue(sql.contains("gl.[date] > GETDATE()"),
                "Count query must filter out expired listings using GETDATE()");
    }

    @Test
    void shouldOnlyReturnOpenListingsInResultsQuery() throws Exception {
        List<Long> sportIds = List.of(3L);
        BrowseFilter filter = new BrowseFilter();
        List<Object> params = new ArrayList<>();

        String sql = (String) buildBrowseQuery.invoke(dao, sportIds, filter, params, false);

        assertTrue(sql.contains("gl.[status] = 'OPEN'"),
                "Results query must only return OPEN listings");
    }

    @Test
    void shouldOnlyReturnOpenListingsInCountQuery() throws Exception {
        List<Long> sportIds = List.of(3L);
        BrowseFilter filter = new BrowseFilter();
        List<Object> params = new ArrayList<>();

        String sql = (String) buildBrowseQuery.invoke(dao, sportIds, filter, params, true);

        assertTrue(sql.contains("gl.[status] = 'OPEN'"),
                "Count query must only return OPEN listings");
    }

    @Test
    void shouldExcludePrivateListingsInResultsQuery() throws Exception {
        List<Long> sportIds = List.of(1L);
        BrowseFilter filter = new BrowseFilter();
        List<Object> params = new ArrayList<>();

        String sql = (String) buildBrowseQuery.invoke(dao, sportIds, filter, params, false);

        assertTrue(sql.contains("gl.[is_private] = 0"),
                "Results query must exclude private listings");
    }

    @Test
    void shouldExcludePrivateListingsInCountQuery() throws Exception {
        List<Long> sportIds = List.of(1L);
        BrowseFilter filter = new BrowseFilter();
        List<Object> params = new ArrayList<>();

        String sql = (String) buildBrowseQuery.invoke(dao, sportIds, filter, params, true);

        assertTrue(sql.contains("gl.[is_private] = 0"),
                "Count query must exclude private listings");
    }

    @Test
    void shouldApplyConsistentBaseFiltersForBothQueryTypes() throws Exception {
        // Both query types must share the same WHERE clause base filters
        // to ensure pagination count matches results
        List<Long> sportIds = List.of(1L, 2L, 3L);
        BrowseFilter filter = new BrowseFilter();

        List<Object> resultsParams = new ArrayList<>();
        String resultsSql = (String) buildBrowseQuery.invoke(dao, sportIds, filter, resultsParams, false);

        List<Object> countParams = new ArrayList<>();
        String countSql = (String) buildBrowseQuery.invoke(dao, sportIds, filter, countParams, true);

        // Both must contain all three base filters
        for (String sql : List.of(resultsSql, countSql)) {
            assertTrue(sql.contains("gl.[status] = 'OPEN'"), "Must filter by OPEN status");
            assertTrue(sql.contains("gl.[is_private] = 0"), "Must exclude private listings");
            assertTrue(sql.contains("gl.[date] > GETDATE()"), "Must exclude expired listings (A500)");
        }

        // Sport ID parameters must be consistent between count and results
        // (excluding pagination params which only exist in results query)
        assertEquals(sportIds.size(), countParams.size(),
                "Count query params should only contain sport ID placeholders");
    }
}
