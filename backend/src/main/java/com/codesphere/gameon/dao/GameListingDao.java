package com.codesphere.gameon.dao;

import com.codesphere.gameon.model.GameListing;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Optional;

/**
 * Data access for the dbo.game_listing table.
 */
public class GameListingDao extends BaseDao {

    public GameListingDao(DataSource dataSource) {
        super(dataSource);
    }

    /**
     * Checks whether the creator has an incomplete listing whose start time
     * is strictly less than 7200 seconds (2 hours) away from the proposed datetime.
     * Uses DATEDIFF_BIG(SECOND, ...) for exact elapsed-second precision.
     * Exactly 2 hours (7200 seconds) apart is allowed (not a conflict).
     * Uses the provided connection for transactional consistency.
     *
     * @return true if a scheduling conflict exists
     */
    public boolean hasSchedulingConflict(Connection conn, long creatorId, java.time.LocalDateTime proposedDateTime) throws SQLException {
        String sql = "SELECT COUNT(*) FROM [dbo].[game_listing] " +
                "WHERE [creator_id] = ? AND [is_completed] = 0 " +
                "AND ABS(DATEDIFF_BIG(SECOND, [date], ?)) < 7200";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, creatorId);
            ps.setTimestamp(2, Timestamp.valueOf(proposedDateTime));
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
                "([date], [is_completed], [is_private], [location], [skill_level], [creator_id], [format_id]) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setTimestamp(1, Timestamp.valueOf(listing.getDate()));
            ps.setBoolean(2, listing.isCompleted());
            ps.setBoolean(3, listing.isPrivate());
            ps.setString(4, listing.getLocation());
            ps.setString(5, listing.getSkillLevel());
            ps.setLong(6, listing.getCreatorId());
            ps.setLong(7, listing.getFormatId());
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
        String sql = "SELECT [game_listing_id], [date], [is_completed], [is_private], " +
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
                rs.getBoolean("is_completed"),
                rs.getBoolean("is_private"),
                rs.getString("location"),
                rs.getString("skill_level"),
                rs.getLong("creator_id"),
                rs.getLong("format_id")
        );
    }
}
