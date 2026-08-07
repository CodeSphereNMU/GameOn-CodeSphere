package com.codesphere.gameon.dao;

import com.codesphere.gameon.dto.BrowseFilter;
import com.codesphere.gameon.dto.BrowseListingDto;
import com.codesphere.gameon.model.GameListing;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data access for the dbo.game_listing table.
 */
public class GameListingDao extends BaseDao {

    public GameListingDao(DataSource dataSource) {
        super(dataSource);
    }

    /**
     * Checks whether the user has a scheduling conflict with the proposed session.
     * A conflict exists when the proposed session window [newStart, newEnd] or its
     * 60-minute travel buffer overlaps with any existing session window or buffer
     * for listings where the user is either the creator or an ACCEPTED game_joiner.
     *
     * Only non-cancelled, non-completed listings are considered.
     * Equality at boundaries is allowed (not a conflict).
     *
     * @param conn             transactional connection
     * @param userId           the user creating the listing
     * @param proposedStart    proposed session start time
     * @param proposedEnd      proposed session end time (start + duration)
     * @return true if a scheduling conflict exists
     */
    public boolean hasSchedulingConflict(Connection conn, long userId, LocalDateTime proposedStart, LocalDateTime proposedEnd) throws SQLException {
        // The conflict zone for an existing session is [existing.date, existing.end_time + 60 min].
        // The conflict zone for the proposed session is [proposedStart, proposedEnd + 60 min].
        // Two sessions conflict if their conflict zones overlap (exclusive boundaries — equality allowed).
        //
        // Overlap condition: existingStart < proposedEnd + 60 min AND proposedStart < existingEnd + 60 min
        // (strict inequality means equality at boundary = no conflict)
        String sql = "SELECT COUNT(*) FROM (" +
                "    SELECT gl.[date] AS existing_start, gl.[end_time] AS existing_end " +
                "    FROM [dbo].[game_listing] gl " +
                "    WHERE gl.[creator_id] = ? " +
                "      AND gl.[status] IN ('OPEN','CONFIRMED') " +
                "    UNION " +
                "    SELECT gl.[date] AS existing_start, gl.[end_time] AS existing_end " +
                "    FROM [dbo].[game_listing] gl " +
                "    INNER JOIN [dbo].[game_joiner] gj ON gl.[game_listing_id] = gj.[game_listing_id] " +
                "    WHERE gj.[user_id] = ? AND gj.[status] = 'ACCEPTED' " +
                "      AND gl.[status] IN ('OPEN','CONFIRMED') " +
                ") AS sessions " +
                "WHERE sessions.existing_start < DATEADD(MINUTE, 60, ?) " +
                "  AND ? < DATEADD(MINUTE, 60, sessions.existing_end)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, userId);
            ps.setTimestamp(3, Timestamp.valueOf(proposedEnd));   // proposedEnd + 60 min buffer
            ps.setTimestamp(4, Timestamp.valueOf(proposedStart)); // proposedStart
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Inserts a new game listing. Returns the generated game_listing_id.
     * Uses the provided connection for transactional consistency.
     */
    public long insert(Connection conn, GameListing listing) throws SQLException {
        String sql = "INSERT INTO [dbo].[game_listing] " +
                "([date], [end_time], [status], [is_private], [location], [skill_level], [creator_id], [format_id]) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setTimestamp(1, Timestamp.valueOf(listing.getDate()));
            ps.setTimestamp(2, Timestamp.valueOf(listing.getEndTime()));
            ps.setString(3, listing.getStatus());
            ps.setBoolean(4, listing.isPrivate());
            ps.setString(5, listing.getLocation());
            ps.setString(6, listing.getSkillLevel());
            ps.setLong(7, listing.getCreatorId());
            ps.setLong(8, listing.getFormatId());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new SQLException("Failed to retrieve generated game_listing_id");
            }
        }
    }

    /**
     * Finds a game listing by its ID.
     */
    public Optional<GameListing> findById(long gameListingId) {
        String sql = "SELECT [game_listing_id], [date], [end_time], [status], [is_private], " +
                "[location], [skill_level], [creator_id], [format_id] " +
                "FROM [dbo].[game_listing] WHERE [game_listing_id] = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, gameListingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding game listing by ID: {}", gameListingId, e);
            throw new RuntimeException("Database error", e);
        }
        return Optional.empty();
    }

    /**
     * Returns a paginated list of browsable listings matching the given filters.
     * Only returns OPEN, public, future listings for the specified sport IDs.
     *
     * @param userSportIds list of sport IDs on the user's profile
     * @param filter       pagination and optional filter parameters
     * @return list of BrowseListingDto for the requested page
     */
    public List<BrowseListingDto> findBrowseListings(List<Long> userSportIds, BrowseFilter filter) {
        if (userSportIds == null || userSportIds.isEmpty()) {
            return List.of();
        }

        List<Object> params = new ArrayList<>();
        String sql = buildBrowseQuery(userSportIds, filter, params, false);

        List<BrowseListingDto> results = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setParameters(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
                while (rs.next()) {
                    BrowseListingDto dto = new BrowseListingDto();
                    dto.setGameListingId(rs.getLong("game_listing_id"));
                    dto.setSportName(rs.getString("sport_name"));
                    dto.setFormatName(rs.getString("format_name"));
                    dto.setSkillLevel(rs.getString("skill_level"));

                    Timestamp startTs = rs.getTimestamp("date");
                    Timestamp endTs = rs.getTimestamp("end_time");
                    LocalDateTime startDt = startTs.toLocalDateTime();
                    LocalDateTime endDt = endTs.toLocalDateTime();

                    dto.setDate(startDt.toLocalDate().toString());
                    String sessionWindow = startDt.format(timeFormatter)
                            + "\u2013" + endDt.format(timeFormatter);
                    dto.setSessionWindow(sessionWindow);

                    dto.setLocation(rs.getString("location"));
                    dto.setSpotsFilled(rs.getInt("spots_filled"));
                    dto.setTotalSpots(rs.getInt("no_players"));
                    dto.setCreatorUsername(rs.getString("creator_username"));
                    results.add(dto);
                }
            }
        } catch (SQLException e) {
            logger.error("Error executing findBrowseListings", e);
            throw new RuntimeException("Database error", e);
        }
        return results;
    }

    /**
     * Returns the total count of browsable listings matching the given filters.
     * Uses the same filtering logic as findBrowseListings but without pagination.
     *
     * @param userSportIds list of sport IDs on the user's profile
     * @param filter       optional filter parameters
     * @return total number of matching listings
     */
    public long countBrowseListings(List<Long> userSportIds, BrowseFilter filter) {
        if (userSportIds == null || userSportIds.isEmpty()) {
            return 0;
        }

        List<Object> params = new ArrayList<>();
        String sql = buildBrowseQuery(userSportIds, filter, params, true);

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setParameters(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error executing countBrowseListings", e);
            throw new RuntimeException("Database error", e);
        }
        return 0;
    }

    /**
     * Builds the browse SQL query dynamically based on filters.
     * When countOnly is true, returns a COUNT(*) query without ORDER BY or pagination.
     */
    private String buildBrowseQuery(List<Long> userSportIds, BrowseFilter filter,
                                    List<Object> params, boolean countOnly) {
        StringBuilder sql = new StringBuilder();

        // Spots filled subquery used in SELECT and optionally in WHERE
        String spotsFilled = "(SELECT COUNT(*) FROM [dbo].[game_joiner] gj " +
                "WHERE gj.[game_listing_id] = gl.[game_listing_id] AND gj.[status] = 'ACCEPTED')";

        if (countOnly) {
            sql.append("SELECT COUNT(*) FROM [dbo].[game_listing] gl ");
        } else {
            sql.append("SELECT gl.[game_listing_id], gl.[date], gl.[end_time], gl.[skill_level], ");
            sql.append("gl.[location], sf.[format_name], sf.[no_players], s.[sport_name], ");
            sql.append("u.[username] AS creator_username, ");
            sql.append(spotsFilled).append(" AS spots_filled ");
            sql.append("FROM [dbo].[game_listing] gl ");
        }

        if (countOnly) {
            sql.append("INNER JOIN [dbo].[sport_format] sf ON gl.[format_id] = sf.[format_id] ");
        } else {
            sql.append("INNER JOIN [dbo].[sport_format] sf ON gl.[format_id] = sf.[format_id] ");
            sql.append("INNER JOIN [dbo].[sport] s ON sf.[sport_id] = s.[sport_id] ");
            sql.append("INNER JOIN [dbo].[users] u ON gl.[creator_id] = u.[user_id] ");
        }

        // For count query, we still need sport_format join for sport_id filter and hideFull
        if (countOnly) {
            // sport join only needed if not filtering by sportId (we need sport_id from sport_format)
            // Actually sport_format already has sport_id, no need for sport table in count
        }

        // Base WHERE conditions
        sql.append("WHERE gl.[status] = 'OPEN' ");
        sql.append("AND gl.[is_private] = 0 ");
        sql.append("AND gl.[date] > GETDATE() ");

        // Sport IDs IN clause
        sql.append("AND sf.[sport_id] IN (");
        for (int i = 0; i < userSportIds.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("?");
            params.add(userSportIds.get(i));
        }
        sql.append(") ");

        // Optional: sportId filter
        if (filter.getSportId() != null) {
            sql.append("AND sf.[sport_id] = ? ");
            params.add(filter.getSportId());
        }

        // Optional: skillLevel filter
        if (filter.getSkillLevel() != null && !filter.getSkillLevel().isBlank()) {
            sql.append("AND gl.[skill_level] = ? ");
            params.add(filter.getSkillLevel());
        }

        // Optional: date filter (single date)
        if (filter.getDate() != null) {
            sql.append("AND CAST(gl.[date] AS DATE) = ? ");
            params.add(Date.valueOf(filter.getDate()));
        }

        // Optional: hideFull filter
        if (filter.isHideFull()) {
            sql.append("AND ").append(spotsFilled).append(" < sf.[no_players] ");
        }

        // Pagination (only for non-count queries)
        if (!countOnly) {
            sql.append("ORDER BY gl.[date] ASC ");
            int offset = (filter.getPage() - 1) * filter.getSize();
            sql.append("OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
            params.add(offset);
            params.add(filter.getSize());
        }

        return sql.toString();
    }

    /**
     * Sets parameters on a PreparedStatement from the dynamic parameter list.
     */
    private void setParameters(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object param = params.get(i);
            if (param instanceof Long) {
                ps.setLong(i + 1, (Long) param);
            } else if (param instanceof Integer) {
                ps.setInt(i + 1, (Integer) param);
            } else if (param instanceof String) {
                ps.setString(i + 1, (String) param);
            } else if (param instanceof Date) {
                ps.setDate(i + 1, (Date) param);
            } else {
                ps.setObject(i + 1, param);
            }
        }
    }

    private GameListing mapRow(ResultSet rs) throws SQLException {
        return new GameListing(
                rs.getLong("game_listing_id"),
                rs.getTimestamp("date").toLocalDateTime(),
                rs.getTimestamp("end_time").toLocalDateTime(),
                rs.getString("status"),
                rs.getBoolean("is_private"),
                rs.getString("location"),
                rs.getString("skill_level"),
                rs.getLong("creator_id"),
                rs.getLong("format_id")
        );
    }
}
