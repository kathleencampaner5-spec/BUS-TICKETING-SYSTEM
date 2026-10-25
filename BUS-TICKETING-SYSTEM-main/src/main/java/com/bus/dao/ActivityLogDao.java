package com.bus.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ActivityLogDao {

    public void log(int userId, String action, String description) throws SQLException {
        String sql = "INSERT INTO activity_logs (user_id, action, description) VALUES (?, ?, ?)";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, action);
            ps.setString(3, description);
            ps.executeUpdate();
        }
    }
}
