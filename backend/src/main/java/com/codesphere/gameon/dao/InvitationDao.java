package com.codesphere.gameon.dao;

import com.codesphere.gameon.model.Invitation;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
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
}
