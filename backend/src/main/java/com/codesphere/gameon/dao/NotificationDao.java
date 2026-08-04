package com.codesphere.gameon.dao;

import com.codesphere.gameon.model.Notification;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

/**
 * Data access for the dbo.notification table.
 */
public class NotificationDao extends BaseDao {

    public NotificationDao(DataSource dataSource) {
        super(dataSource);
    }

    /**
     * Inserts a batch of notifications within the given transaction.
     */
    public void insertBatch(Connection conn, List<Notification> notifications) throws SQLException {
        if (notifications == null || notifications.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO [dbo].[notification] ([is_read], [text], [type_of_notification], [recipient_id], [game_listing_id]) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Notification n : notifications) {
                ps.setBoolean(1, n.isRead());
                ps.setString(2, n.getText());
                ps.setString(3, n.getTypeOfNotification());
                ps.setLong(4, n.getRecipientId());
                if (n.getGameListingId() != null) {
                    ps.setLong(5, n.getGameListingId());
                } else {
                    ps.setNull(5, Types.BIGINT);
                }
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
