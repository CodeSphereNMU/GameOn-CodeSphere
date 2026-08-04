package com.codesphere.gameon.dao;

import com.codesphere.gameon.model.SportFormat;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data access for the dbo.sport_format table.
 */
public class SportFormatDao extends BaseDao {

    public SportFormatDao(DataSource dataSource) {
        super(dataSource);
    }

    /**
     * Returns all formats for a given sport.
     */
    public List<SportFormat> findFormatsBySportId(long sportId) {
        String sql = "SELECT [format_id], [format_name], [has_positions], [no_players], [duration_minutes], [sport_id] " +
                "FROM [dbo].[sport_format] WHERE [sport_id] = ? ORDER BY [format_name]";
        List<SportFormat> formats = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, sportId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    formats.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding formats for sport: {}", sportId, e);
            throw new RuntimeException("Database error", e);
        }
        return formats;
    }

    /**
     * Finds a format by its ID.
     */
    public Optional<SportFormat> findById(long formatId) {
        String sql = "SELECT [format_id], [format_name], [has_positions], [no_players], [duration_minutes], [sport_id] " +
                "FROM [dbo].[sport_format] WHERE [format_id] = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, formatId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding format by ID: {}", formatId, e);
            throw new RuntimeException("Database error", e);
        }
        return Optional.empty();
    }

    private SportFormat mapRow(ResultSet rs) throws SQLException {
        return new SportFormat(
                rs.getLong("format_id"),
                rs.getString("format_name"),
                rs.getBoolean("has_positions"),
                rs.getInt("no_players"),
                rs.getInt("duration_minutes"),
                rs.getLong("sport_id")
        );
    }
}
