package com.edu.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class NotificationUtil {
    public static void createResourceNotification(Connection conn, int userId, String title, String content) throws SQLException {
        String sql = "INSERT INTO notification (user_id, title, content, type, create_time) VALUES (?, ?, ?, 'resource', NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, title);
            ps.setString(3, content);
            ps.executeUpdate();
        }
    }
}

