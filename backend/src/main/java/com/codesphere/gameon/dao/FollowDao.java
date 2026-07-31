package com.codesphere.gameon.dao;

import com.codesphere.gameon.model.User;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Data access for mutual followers (friends) via dbo.follow.
 */
public class FollowDao extends BaseDao {

    public FollowDao(DataSource dataSource) {
        super(dataSource);
    }

    /**
     * Returns mutual followers (friends) for the given user.
     * A mutual follow exists when user A follows user B AND user B follows user A.
     */
    public List<User> findMutualFollowers(long userId) {
        String sql = "SELECT u.[user_id], u.[username] " +
                "FROM [dbo].[users] u " +
                "WHERE u.[user_id] IN (" +
                "    SELECT f1.[followed_user_id] " +
                "    FROM [dbo].[follow] f1 " +
                "    INNER JOIN [dbo].[follow] f2 " +
                "        ON f1.[followed_user_id] = f2.[follower_user_id] " +
                "        AND f1.[follower_user_id] = f2.[followed_user_id] " +
                "    WHERE f1.[follower_user_id] = ?" +
                ") " +
                "ORDER BY u.[username]";
        List<User> friends = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User friend = new User();
                    friend.setUserId(rs.getLong("user_id"));
                    friend.setUsername(rs.getString("username"));
                    friends.add(friend);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding mutual followers for user: {}", userId, e);
            throw new RuntimeException("Database error", e);
        }
        return friends;
    }

    /**
     * Returns the set of user IDs that are mutual followers of the given user.
     */
    public Set<Long> findMutualFollowerIds(long userId) {
        String sql = "SELECT f1.[followed_user_id] " +
                "FROM [dbo].[follow] f1 " +
                "INNER JOIN [dbo].[follow] f2 " +
                "    ON f1.[followed_user_id] = f2.[follower_user_id] " +
                "    AND f1.[follower_user_id] = f2.[followed_user_id] " +
                "WHERE f1.[follower_user_id] = ?";
        Set<Long> friendIds = new HashSet<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    friendIds.add(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding mutual follower IDs for user: {}", userId, e);
            throw new RuntimeException("Database error", e);
        }
        return friendIds;
    }
}
