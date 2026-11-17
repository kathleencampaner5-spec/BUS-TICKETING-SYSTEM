package com.bus.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {

    private static final String URL = "jdbc:sqlite:bus_ticketing.db";

    /**
     * Opens a connection with SQLite foreign key enforcement turned on.
     * SQLite disables foreign keys by default and the setting is per-connection,
     * so the PRAGMA must run every time a connection is created.
     */
    public static Connection getConnection() throws SQLException {
        Connection con = DriverManager.getConnection(URL);
        try (Statement stmt = con.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        } catch (SQLException e) {
            con.close();
            throw e;
        }
        return con;
    }
}
