package com.bus.dao;

import com.bus.model.ActivityLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * All SQL for the activity_logs table lives here (no SQL in the UI layer).
 * Every query uses a parameterized PreparedStatement.
 */
public class ActivityLogDao {

    // Local time is stored so the console shows the same clock as the user's computer.
    private static final String INSERT_SQL =
            "INSERT INTO activity_logs (user_id, action_type, description, timestamp) "
                    + "VALUES (?, ?, ?, datetime('now', 'localtime'))";

    private static final String SELECT_COLUMNS =
            "SELECT log_id, user_id, action_type, description, timestamp FROM activity_logs ";

    /** Inserts one log entry using its own connection. */
    public void logAction(int userId, String actionType, String description) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection()) {
            logAction(con, userId, actionType, description);
        }
    }

    /**
     * Inserts one log entry on an existing connection. Other DAOs call this inside their
     * own transaction so the business change and its audit entry are saved together.
     */
    public void logAction(Connection con, int userId, String actionType, String description)
            throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(INSERT_SQL)) {
            ps.setInt(1, userId);
            ps.setString(2, actionType);
            ps.setString(3, description);
            ps.executeUpdate();
        }
    }

    /** @return every log, newest first */
    public List<ActivityLog> getAllLogs() throws SQLException {
        String sql = SELECT_COLUMNS + "ORDER BY timestamp DESC, log_id DESC";

        List<ActivityLog> logs = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                logs.add(mapLog(rs));
            }
        }
        return logs;
    }

    /** @return the logs of one user, newest first */
    public List<ActivityLog> getLogsByUserId(int userId) throws SQLException {
        String sql = SELECT_COLUMNS + "WHERE user_id = ? ORDER BY timestamp DESC, log_id DESC";

        List<ActivityLog> logs = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapLog(rs));
                }
            }
        }
        return logs;
    }

    /**
     * Deletes every log and, in the same transaction, records who cleared them so the
     * audit trail is never silently empty.
     * @return how many logs were deleted
     */
    public int clearLogs(int adminId) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                int deleted;
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM activity_logs")) {
                    deleted = ps.executeUpdate();
                }
                logAction(con, adminId, "LOGS_CLEARED",
                        "Admin [" + adminId + "] cleared " + deleted + " log entries");
                con.commit();
                return deleted;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    private ActivityLog mapLog(ResultSet rs) throws SQLException {
        return new ActivityLog(
                rs.getInt("log_id"),
                rs.getInt("user_id"),
                rs.getString("action_type"),
                rs.getString("description"),
                rs.getString("timestamp")
        );
    }
}
