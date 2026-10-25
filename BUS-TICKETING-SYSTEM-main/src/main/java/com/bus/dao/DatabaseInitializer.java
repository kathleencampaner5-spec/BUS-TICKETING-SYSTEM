package com.bus.dao;

import com.bus.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates every table of the approved ERD if it does not exist yet.
 * Parent tables are created before the tables that reference them.
 *
 * Account statuses: PENDING (awaiting admin approval), APPROVED, INACTIVE.
 */
public class DatabaseInitializer {

    // Default administrator so the very first approval is possible.
    // Change this password after the first login.
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin123";

    private static String usersTableDdl(String tableName) {
        return """
                CREATE TABLE IF NOT EXISTS %s (
                    user_id       INTEGER PRIMARY KEY AUTOINCREMENT,
                    first_name    TEXT NOT NULL,
                    last_name     TEXT NOT NULL,
                    phone_number  TEXT NOT NULL,
                    username      TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL,
                    role          TEXT NOT NULL
                        CHECK (role IN ('ADMIN', 'CASHIER', 'PASSENGER')),
                    status        TEXT NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING', 'APPROVED', 'INACTIVE'))
                )
                """.formatted(tableName);
    }

    public static void initialize() throws SQLException {
        String[] statements = {
                usersTableDdl("users"),
                """
                CREATE TABLE IF NOT EXISTS buses (
                    bus_id       INTEGER PRIMARY KEY AUTOINCREMENT,
                    bus_number   TEXT NOT NULL UNIQUE,
                    plate_number TEXT NOT NULL UNIQUE,
                    bus_type     TEXT NOT NULL,
                    status       TEXT NOT NULL DEFAULT 'AVAILABLE'
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS routes (
                    route_id           INTEGER PRIMARY KEY AUTOINCREMENT,
                    origin             TEXT NOT NULL,
                    destination        TEXT NOT NULL,
                    distance_km        REAL NOT NULL,
                    estimated_duration TEXT NOT NULL
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS trips (
                    trip_id            INTEGER PRIMARY KEY AUTOINCREMENT,
                    bus_id             INTEGER NOT NULL,
                    route_id           INTEGER NOT NULL,
                    departure_datetime TIMESTAMP NOT NULL,
                    fare_amount        REAL NOT NULL,
                    status             TEXT NOT NULL DEFAULT 'SCHEDULED',
                    FOREIGN KEY (bus_id)   REFERENCES buses(bus_id),
                    FOREIGN KEY (route_id) REFERENCES routes(route_id)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS tickets (
                    ticket_id    INTEGER PRIMARY KEY AUTOINCREMENT,
                    trip_id      INTEGER NOT NULL,
                    user_id      INTEGER NOT NULL,
                    seat_number  TEXT NOT NULL,
                    booking_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    status       TEXT NOT NULL DEFAULT 'BOOKED',
                    FOREIGN KEY (trip_id) REFERENCES trips(trip_id),
                    FOREIGN KEY (user_id) REFERENCES users(user_id)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS activity_logs (
                    log_id      INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id     INTEGER NOT NULL,
                    action      TEXT NOT NULL,
                    description TEXT,
                    timestamp   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES users(user_id)
                )
                """
        };

        try (Connection con = DatabaseConnection.getConnection();
             Statement stmt = con.createStatement()) {
            for (String sql : statements) {
                stmt.execute(sql);
            }
            migrateLegacyStatusValues(con);
            seedDefaultAdmin(con);
        }
    }

    /**
     * Older databases were created with CHECK (status IN ('PENDING','ACTIVE','INACTIVE')).
     * SQLite cannot alter a CHECK constraint, so the users table is rebuilt once and
     * existing 'ACTIVE' rows are converted to 'APPROVED'. Data and IDs are preserved.
     */
    private static void migrateLegacyStatusValues(Connection con) throws SQLException {
        String currentDdl = null;
        try (Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'users'")) {
            if (rs.next()) {
                currentDdl = rs.getString(1);
            }
        }
        if (currentDdl == null || !currentDdl.contains("'ACTIVE'")) {
            return; // already on the new schema
        }

        try (Statement stmt = con.createStatement()) {
            // Must be switched off outside a transaction so DROP TABLE does not hit child FKs.
            stmt.execute("PRAGMA foreign_keys = OFF");
            con.setAutoCommit(false);
            try {
                // Official SQLite procedure: build new table, copy, drop old, rename new.
                // (Renaming the OLD table first would rewrite child foreign keys to point at it.)
                stmt.execute(usersTableDdl("users_new"));
                stmt.execute("""
                        INSERT INTO users_new (user_id, first_name, last_name, phone_number,
                                               username, password_hash, role, status)
                        SELECT user_id, first_name, last_name, phone_number,
                               username, password_hash, role,
                               CASE WHEN status = 'ACTIVE' THEN 'APPROVED' ELSE status END
                        FROM users
                        """);
                stmt.execute("DROP TABLE users");
                stmt.execute("ALTER TABLE users_new RENAME TO users");
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
                stmt.execute("PRAGMA foreign_keys = ON");
            }
        }
    }

    /** Creates an APPROVED admin account only when no admin exists yet. */
    private static void seedDefaultAdmin(Connection con) throws SQLException {
        try (PreparedStatement check = con.prepareStatement(
                "SELECT 1 FROM users WHERE role = ?")) {
            check.setString(1, "ADMIN");
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }

        String sql = """
                INSERT INTO users (first_name, last_name, phone_number,
                                   username, password_hash, role, status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "System");
            ps.setString(2, "Administrator");
            ps.setString(3, "N/A");
            ps.setString(4, DEFAULT_ADMIN_USERNAME);
            ps.setString(5, PasswordUtil.hash(DEFAULT_ADMIN_PASSWORD));
            ps.setString(6, "ADMIN");
            ps.setString(7, "APPROVED");
            ps.executeUpdate();
        }
    }
}
