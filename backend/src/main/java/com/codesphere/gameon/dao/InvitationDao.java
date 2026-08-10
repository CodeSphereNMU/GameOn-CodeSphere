package com.codesphere.gameon.dao;

import com.codesphere.gameon.model.Invitation;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Data access for the dbo.invitation table.
 */
public class InvitationDao extends BaseDao {

    public InvitationDao(DataSource dataSource) {
        super(dataSource);
    }

    /**
     * Inserts a batch of PENDING invitations within the given transaction.
     * Each invitation links a listing to an invited user.
     */
    public void insertBatch(Connection conn, List<Invitation> invitations) throws SQLException {
        if (invitations == null || invitations.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO [dbo].[invitation] ([game_listing_id], [invitee_id], [status]) " +
                "VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Invitation inv : invitations) {
                ps.setLong(1, inv.getGameListingId());
                ps.setLong(2, inv.getInviteeId());
                ps.setString(3, inv.getStatus());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * Finds the PENDING invitation for a given listing and user.
     * Returns the invitation_id if one exists, or null otherwise.
     * Uses the provided connection for transactional consistency.
     */
    public Long findPendingInvitationId(Connection conn, long gameListingId, long userId) throws SQLException {
        String sql = "SELECT [invitation_id] FROM [dbo].[invitation] " +
                "WHERE [game_listing_id] = ? AND [invitee_id] = ? AND [status] = 'PENDING'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, gameListingId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("invitation_id");
                }
                return null;
            }
        }
    }

    /**
     * Checks whether an invitation exists for the given listing and user.
     * Used by the detail endpoint to verify private listing access.
     */
    public boolean hasInvitation(long gameListingId, long userId) {
        String sql = "SELECT 1 FROM [dbo].[invitation] WHERE [game_listing_id] = ? AND [invitee_id] = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, gameListingId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Error checking invitation for listing: {} user: {}", gameListingId, userId, e);
            throw new RuntimeException("Database error", e);
        }
    }
}
