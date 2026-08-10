package com.codesphere.gameon.dao;

import com.codesphere.gameon.dto.RosterEntryDto;
import com.codesphere.gameon.model.GameJoiner;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
                "([game_listing_id], [user_id], [team], [status], [position_id], [format_id], [alternate_position_id], [join_request_id]) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
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

            if (joiner.getAlternatePositionId() != null) {
                ps.setLong(7, joiner.getAlternatePositionId());
            } else {
                ps.setNull(7, Types.BIGINT);
            }

            // Creator has no join_request_id
            if (joiner.getJoinRequestId() != null) {
                ps.setLong(8, joiner.getJoinRequestId());
            } else {
                ps.setNull(8, Types.BIGINT);
            }

            ps.executeUpdate();
        }
    }

    /**
     * Finds the roster of accepted participants for a given game listing,
     * grouped by team letter (e.g. "A", "B").
     *
     * Each entry includes the participant's username and optional position name.
     * Results are ordered by team then username.
     *
     * @param gameListingId the listing to fetch the roster for
     * @return a map of team letter to list of roster entries
     */
    public Map<String, List<RosterEntryDto>> findRosterByListingId(long gameListingId) {
        String sql = "SELECT gj.[team], u.[username], p.[position_name], ap.[position_name] AS alternate_position_name " +
                "FROM [dbo].[game_joiner] gj " +
                "INNER JOIN [dbo].[users] u ON gj.[user_id] = u.[user_id] " +
                "LEFT JOIN [dbo].[position] p ON gj.[position_id] = p.[position_id] " +
                "LEFT JOIN [dbo].[position] ap ON gj.[alternate_position_id] = ap.[position_id] " +
                "WHERE gj.[game_listing_id] = ? AND gj.[status] = 'ACCEPTED' " +
                "ORDER BY gj.[team], u.[username]";

        Map<String, List<RosterEntryDto>> roster = new LinkedHashMap<>();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, gameListingId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String team = rs.getString("team");
                    String username = rs.getString("username");
                    String positionName = rs.getString("position_name"); // null for non-positional formats
                    String alternatePositionName = rs.getString("alternate_position_name");

                    roster.computeIfAbsent(team, k -> new ArrayList<>())
                            .add(new RosterEntryDto(username, positionName, alternatePositionName));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding roster for listing ID: {}", gameListingId, e);
            throw new RuntimeException("Database error", e);
        }

        return roster;
    }

    /**
     * Counts the number of accepted joiners for a given game listing.
     *
     * @param gameListingId the listing to count accepted joiners for
     * @return the count of accepted joiners
     */
    public int countAcceptedByListingId(long gameListingId) {
        String sql = "SELECT COUNT(*) FROM [dbo].[game_joiner] WHERE [game_listing_id] = ? AND [status] = 'ACCEPTED'";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, gameListingId);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error counting accepted joiners for listing ID: {}", gameListingId, e);
            throw new RuntimeException("Database error", e);
        }
    }

    /**
     * Checks whether a user is already an ACCEPTED game_joiner on the given listing.
     * Uses the provided connection for transactional consistency.
     *
     * @param conn the database connection (caller-managed transaction)
     * @param gameListingId the listing to check
     * @param userId the user to check
     * @return true if the user is an accepted joiner on the listing
     * @throws SQLException if a database error occurs
     */
    public boolean isAcceptedJoiner(Connection conn, long gameListingId, long userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM [dbo].[game_joiner] WHERE [game_listing_id] = ? AND [user_id] = ? AND [status] = 'ACCEPTED'";
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
     * Checks whether a user is already an ACCEPTED game_joiner on the given listing.
     * Non-transactional overload — obtains its own connection from the pool.
     *
     * @param gameListingId the listing to check
     * @param userId the user to check
     * @return true if the user is an accepted joiner on the listing
     */
    public boolean isAcceptedJoiner(long gameListingId, long userId) {
        String sql = "SELECT COUNT(*) FROM [dbo].[game_joiner] WHERE [game_listing_id] = ? AND [user_id] = ? AND [status] = 'ACCEPTED'";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, gameListingId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.error("Error checking accepted joiner for listing ID: {}, user ID: {}", gameListingId, userId, e);
            throw new RuntimeException("Database error", e);
        }
    }
}
