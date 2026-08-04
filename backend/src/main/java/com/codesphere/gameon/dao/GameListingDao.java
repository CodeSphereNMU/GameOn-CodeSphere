package com.codesphere.gameon.dao;

import com.codesphere.gameon.model.GameListing;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
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
