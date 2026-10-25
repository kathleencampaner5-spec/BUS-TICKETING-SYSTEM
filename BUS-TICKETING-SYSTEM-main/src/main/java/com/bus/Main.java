package com.bus;

import com.bus.dao.ActivityLogDao;
import com.bus.dao.DatabaseInitializer;
import com.bus.dao.UserDao;
import com.bus.ui.Authentication;

import java.sql.SQLException;
import java.util.Scanner;

/**
 * Single entry point of the application.
 * Flow: Initialize Database -> Instantiate DAOs & Utilities -> Authentication.start()
 */
public class 1Main {

    public static void main(String[] args) {
        try {
            // 1. Make sure every required table exists before any menu is shown
            DatabaseInitializer.initialize();
        } catch (SQLException e) {
            System.out.println("Database initialization failed: " + e.getMessage());
            return;
        }

        // 2. Create single shared instances
        UserDao userDao = new UserDao();
        ActivityLogDao logDao = new ActivityLogDao();

        try (Scanner sc = new Scanner(System.in)) {
            // 3. Hand over control to the authentication layer
            new Authentication(userDao, logDao, sc).start();
        }
    }
}
