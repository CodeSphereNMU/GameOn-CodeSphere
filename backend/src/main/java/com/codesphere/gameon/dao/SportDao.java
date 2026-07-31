package com.codesphere.gameon.dao;

import com.codesphere.gameon.model.Sport;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for sports, filtered through user_sport_profile.
 */
public class SportDao extends BaseDao {

    public SportDao(DataSource dataSource) {
        super(dataSource);
    }

    /**
     * Returns all sports that the given user has registered in their profile.
     */
    public List<Sport> findSportsByUserId(long userId) {
        String sql = "SELECT s.[sport_id], s.[sport_name] " +
                "FROM [dbo].[sport] s " +
                "INNER JOIN [dbo].[user_sport_profile] usp ON s.[sport_id] = usp.[sport_id] " +
                "WHERE usp.[user_id] = ? " +
                "ORDER BY s.[sport_name]";
        List<Sport> sports = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sports.add(new Sport(rs.getLong("sport_id"), rs.getString("sport_name")));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding sports for user: {}", userId, e);
            throw new RuntimeException("Database error", e);
        }
        return sports;
    }

    /**
     * Checks whether the given user has the specified sport on their profile.
     */
    public boolean userHasSport(long userId, long sportId) {
        String sql = "SELECT 1 FROM [dbo].[user_sport_profile] WHERE [user_id] = ? AND [sport_id] = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, sportId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Error checking user sport: userId={}, sportId={}", userId, sportId, e);
            throw new RuntimeException("Database error", e);
        }
    }

    /**
     * Returns the user's skill level for a given sport, or null if not found.
     */
    public String getUserSkillLevel(long userId, long sportId) {
        String sql = "SELECT [skill_level] FROM [dbo].[user_sport_profile] WHERE [user_id] = ? AND [sport_id] = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, sportId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("skill_level");
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting user skill level: userId={}, sportId={}", userId, sportId, e);
            throw new RuntimeException("Database error", e);
        }
        return null;
    }
}
