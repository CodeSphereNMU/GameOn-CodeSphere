package com.codesphere.gameon.dao;

import com.codesphere.gameon.model.User;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Data access for the dbo.users table.
 */
public class UserDao extends BaseDao {

    public UserDao(DataSource dataSource) {
        super(dataSource);
    }

    /**
     * Finds a user by their username.
     *
     * @param username the username to search for
     * @return an Optional containing the user if found, or empty if not
     */
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT [user_id], [username], [password], [type_of_user] FROM [dbo].[users] WHERE [username] = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding user by username: {}", username, e);
            throw new RuntimeException("Database error", e);
        }
        return Optional.empty();
    }

    /**
     * Finds a user by their ID.
     *
     * @param userId the user ID to search for
     * @return an Optional containing the user if found, or empty if not
     */
    public Optional<User> findById(long userId) {
        String sql = "SELECT [user_id], [username], [password], [type_of_user] FROM [dbo].[users] WHERE [user_id] = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding user by ID: {}", userId, e);
            throw new RuntimeException("Database error", e);
        }
        return Optional.empty();
    }

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getLong("user_id"),
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("type_of_user")
        );
    }
}
