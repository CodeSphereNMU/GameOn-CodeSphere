package com.codesphere.gameon.dao;

import com.codesphere.gameon.model.GameJoiner;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Data access for the dbo.game_joiner table.
 */
public class GameJoinerDao extends BaseDao {

    public GameJoinerDao(DataSource dataSource) {
        super(dataSource);
    }

    /**
     * Inserts the creator as the first accepted participant.
     * Uses the provided connection for transactional consistency.
     */
    public void insertCreator(Connection conn, GameJoiner joiner) throws SQLException {
        String sql = "INSERT INTO [dbo].[game_joiner] " +
                "([game_listing_id], [user_id], [team], [status], [position_id], [format_id], [alternate_format_position]) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, joiner.getGameListingId());
            ps.setLong(2, joiner.getUserId());
            ps.setString(3, joiner.getTeam());
            ps.setString(4, joiner.getStatus());

            if (joiner.getPositionId() != null) {
                ps.setLong(5, joiner.getPositionId());
            } else {
                ps.setNull(5, Types.BIGINT);
            }

            if (joiner.getFormatId() != null) {
                ps.setLong(6, joiner.getFormatId());
            } else {
                ps.setNull(6, Types.BIGINT);
            }

            ps.setString(7, joiner.getAlternateFormatPosition());
            ps.executeUpdate();
        }
    }
}
