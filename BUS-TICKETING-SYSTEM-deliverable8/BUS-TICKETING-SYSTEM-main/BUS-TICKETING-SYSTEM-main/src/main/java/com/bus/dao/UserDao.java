package com.bus.dao;

import com.bus.model.Role;
import com.bus.model.User;
import com.bus.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    private final ActivityLogDao logDao;

    public UserDao(ActivityLogDao logDao) {
        this.logDao = logDao;
    }

    /**
     * Inserts a new user with PENDING status.
     * @return the generated user_id, or -1 if the insert failed
     */
    public int register(String firstName, String lastName, String phoneNumber,
                        String username, String password, Role role)
            throws SQLException {

        String sql = """
                INSERT INTO users (first_name, last_name, phone_number,
                                   username, password_hash, role, status)
                VALUES (?, ?, ?, ?, ?, ?, 'PENDING')
                """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, firstName);
            ps.setString(2, lastName);
            ps.setString(3, phoneNumber);
            ps.setString(4, username);
            ps.setString(5, PasswordUtil.hash(password));
            ps.setString(6, role.name());

            if (ps.executeUpdate() != 1) {
                return -1;
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    public boolean usernameExists(String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** @return the matching user, or null if the credentials are wrong */
    public User login(String username, String password) throws SQLException {
        String sql = """
                SELECT user_id, first_name, last_name, phone_number,
                       username, password_hash, role, status
                FROM users
                WHERE username = ?
                """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && PasswordUtil.verify(password, rs.getString("password_hash"))) {
                    return mapUser(rs);
                }
            }
        }
        return null;
    }

    /** @return every registered account regardless of role or status */
    public List<User> getAllUsers() throws SQLException {
        String sql = """
                SELECT user_id, first_name, last_name, phone_number,
                       username, password_hash, role, status
                FROM users
                ORDER BY user_id
                """;

        List<User> users = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        }
        return users;
    }

    /** @return every account (any role) whose status is PENDING */
    public List<User> getPendingUsers() throws SQLException {
        String sql = """
                SELECT user_id, first_name, last_name, phone_number,
                       username, password_hash, role, status
                FROM users
                WHERE status = ?
                ORDER BY user_id
                """;

        List<User> users = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "PENDING");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(mapUser(rs));
                }
            }
        }
        return users;
    }

    /** @return the user with this ID, or null if none exists */
    public User getUserById(int userId) throws SQLException {
        String sql = """
                SELECT user_id, first_name, last_name, phone_number,
                       username, password_hash, role, status
                FROM users
                WHERE user_id = ?
                """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapUser(rs) : null;
            }
        }
    }

    /**
     * Changes a user's status from PENDING to APPROVED and logs USER_APPROVED
     * in the same transaction.
     * @param adminId the admin who performs the approval
     * @return true if a row was updated
     */
    public boolean approveUser(int adminId, int userId) throws SQLException {
        String sql = "UPDATE users SET status = 'APPROVED' WHERE user_id = ? AND status = 'PENDING'";

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, userId);
                if (ps.executeUpdate() != 1) {
                    con.rollback();
                    return false;
                }
                logDao.logAction(con, adminId, "USER_APPROVED",
                        "Admin [" + adminId + "] approved account for User ID " + userId);
                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /**
     * Updates a user's role and account status and logs USER_UPDATED
     * in the same transaction. Values are normalized before being stored.
     * @param adminId the admin who performs the change
     * @return true when exactly one account was updated
     */
    public boolean updateUserRoleAndStatus(int adminId, int userId, String newRole, String newStatus)
            throws SQLException {
        String sql = "UPDATE users SET role = ?, status = ? WHERE user_id = ?";
        String role = newRole.toUpperCase();
        String status = newStatus.toUpperCase();

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, role);
                ps.setString(2, status);
                ps.setInt(3, userId);
                if (ps.executeUpdate() != 1) {
                    con.rollback();
                    return false;
                }
                logDao.logAction(con, adminId, "USER_UPDATED",
                        "Admin [" + adminId + "] changed role of User " + userId + " to " + role
                                + " and status to " + status);
                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("user_id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("phone_number"),
                rs.getString("username"),
                rs.getString("password_hash"),
                Role.valueOf(rs.getString("role")),
                rs.getString("status")
        );
    }
}
