package com.codesphere.gameon.dao;

import com.codesphere.gameon.model.Position;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for positions via dbo.format_position and dbo.position.
 */
public class PositionDao extends BaseDao {

    public PositionDao(DataSource dataSource) {
        super(dataSource);
    }

    /**
     * Returns all positions valid for a given format.
     */
    public List<Position> findPositionsByFormatId(long formatId) {
        String sql = "SELECT p.[position_id], p.[position_name] " +
                "FROM [dbo].[position] p " +
                "INNER JOIN [dbo].[format_position] fp ON p.[position_id] = fp.[position_id] " +
                "WHERE fp.[format_id] = ? " +
                "ORDER BY p.[position_name]";
        List<Position> positions = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, formatId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    positions.add(new Position(rs.getLong("position_id"), rs.getString("position_name")));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding positions for format: {}", formatId, e);
            throw new RuntimeException("Database error", e);
        }
        return positions;
    }

    /**
     * Checks whether a specific position belongs to a specific format.
     */
    public boolean positionBelongsToFormat(long positionId, long formatId) {
        String sql = "SELECT 1 FROM [dbo].[format_position] WHERE [position_id] = ? AND [format_id] = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, positionId);
            ps.setLong(2, formatId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Error checking position belongs to format: positionId={}, formatId={}", positionId, formatId, e);
            throw new RuntimeException("Database error", e);
        }
    }
}
