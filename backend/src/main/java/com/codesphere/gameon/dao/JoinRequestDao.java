package com.codesphere.gameon.dao;

import com.codesphere.gameon.model.JoinRequest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

/**
 * Data access for the dbo.join_request table.
 */
public class JoinRequestDao extends BaseDao {

    public JoinRequestDao(DataSource dataSource) {
        super(dataSource);
    }

    /**
     * Inserts a PENDING join request. Returns the generated join_request_id.
     * Uses the provided connection for transactional consistency.
     */
    public long insert(Connection conn, JoinRequest joinRequest) throws SQLException {
        String sql = "INSERT INTO [dbo].[join_request] " +
                "([game_listing_id], [user_id], [format_id], [team], [position_id], [alternate_position_id], [invitation_id], [status]) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, joinRequest.getGameListingId());
            ps.setLong(2, joinRequest.getUserId());
            ps.setLong(3, joinRequest.getFormatId());
            ps.setString(4, joinRequest.getTeam());

            if (joinRequest.getPositionId() != null) {
                ps.setLong(5, joinRequest.getPositionId());
            } else {
                ps.setNull(5, Types.BIGINT);
            }

            if (joinRequest.getAlternatePositionId() != null) {
                ps.setLong(6, joinRequest.getAlternatePositionId());
            } else {
                ps.setNull(6, Types.BIGINT);
            }

            if (joinRequest.getInvitationId() != null) {
                ps.setLong(7, joinRequest.getInvitationId());
            } else {
                ps.setNull(7, Types.BIGINT);
            }

            ps.setString(8, "PENDING");

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                throw new SQLException("Insert succeeded but no generated key was returned");
            }
        }
    }

    /**
     * Checks whether the user already has a PENDING join_request for the given listing.
     * Mirrors the filtered unique index UX_join_request_one_pending condition.
     * Uses the provided connection for transactional consistency.
     */
    public boolean hasPendingRequest(Connection conn, long gameListingId, long userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM [dbo].[join_request] " +
                "WHERE [game_listing_id] = ? AND [user_id] = ? AND [status] = 'PENDING'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, gameListingId);
            ps.setLong(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Checks whether the user already has a PENDING join_request for the given listing.
     * Non-transactional overload — obtains its own connection from the pool.
     *
     * @param gameListingId the listing to check
     * @param userId the user to check
     * @return true if the user has a pending request for the listing
     */
    public boolean hasPendingRequest(long gameListingId, long userId) {
        String sql = "SELECT COUNT(*) FROM [dbo].[join_request] " +
                "WHERE [game_listing_id] = ? AND [user_id] = ? AND [status] = 'PENDING'";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, gameListingId);
            ps.setLong(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.error("Error checking pending request for listing ID: {}, user ID: {}", gameListingId, userId, e);
            throw new RuntimeException("Database error", e);
        }
    }
}
