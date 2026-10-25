package com.bus.ui;

import com.bus.dao.ActivityLogDao;
import com.bus.dao.UserDao;
import com.bus.model.User;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private final User user;
    private final UserDao userDao;
    private final ActivityLogDao logDao;
    private final Scanner sc;

    public AdminMenu(User user, UserDao userDao, ActivityLogDao logDao, Scanner sc) {
        this.user = user;
        this.userDao = userDao;
        this.logDao = logDao;
        this.sc = sc;
    }

    public void show() {
        System.out.println("\nWelcome, " + user.fullName() + " (ADMIN)");
        System.out.println("Status: " + user.status());

        while (true) {
            System.out.println("\n=== ADMIN MENU ===");
            System.out.println("1. View All System Users");
            System.out.println("2. View Pending Registrations");
            System.out.println("3. Approve User Account");
            System.out.println("4. View My Account");
            System.out.println("5. Logout");
            System.out.print("Choose: ");

            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> viewAllUsers();
                case "2" -> viewPendingUsers();
                case "3" -> approveUserAccount();
                case "4" -> viewMyAccount();
                case "5" -> {
                    System.out.println("Logged out successfully.");
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private void viewAllUsers() {
        try {
            System.out.println("\n--- ALL SYSTEM USERS ---");
            printUserTable(userDao.getAllUsers());
        } catch (SQLException e) {
            System.out.println("Could not load users: " + e.getMessage());
        }
    }

    private void viewPendingUsers() {
        try {
            System.out.println("\n--- PENDING REGISTRATIONS ---");
            printUserTable(userDao.getPendingUsers());
        } catch (SQLException e) {
            System.out.println("Could not load pending users: " + e.getMessage());
        }
    }

    private void approveUserAccount() {
        try {
            List<User> pending = userDao.getPendingUsers();

            System.out.println("\n--- APPROVE USER ACCOUNT ---");
            printUserTable(pending);

            if (pending.isEmpty()) {
                return;
            }

            while (true) {
                System.out.print("Enter Target User ID to approve (0 to cancel): ");
                String input = sc.nextLine().trim();

                int targetId;
                try {
                    targetId = Integer.parseInt(input);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input. Please enter a numeric User ID.");
                    continue;
                }

                if (targetId == 0) {
                    System.out.println("Approval cancelled.");
                    return;
                }

                User target = findById(pending, targetId);
                if (target == null) {
                    System.out.println("User ID " + targetId + " is not in the pending list.");
                    continue;
                }

                if (userDao.approveUser(targetId)) {
                    logDao.log(user.userId(), "APPROVE_USER",
                            "Approved " + target.role() + " account '" + target.username()
                                    + "' (ID " + targetId + ")");
                    System.out.println("SUCCESS: User '" + target.username() + "' (ID "
                            + targetId + ", " + target.role() + ") has been APPROVED.");
                } else {
                    System.out.println("Approval failed. The account may have been changed.");
                }
                return;
            }
        } catch (SQLException e) {
            System.out.println("Approval failed: " + e.getMessage());
        }
    }

    private User findById(List<User> users, int id) {
        for (User u : users) {
            if (u.userId() == id) {
                return u;
            }
        }
        return null;
    }

    private void viewMyAccount() {
        System.out.println("Name: " + user.fullName());
        System.out.println("Username: " + user.username());
        System.out.println("Phone: " + user.phoneNumber());
        System.out.println("Role: " + user.role());
        System.out.println("Status: " + user.status());
    }

    /** Renders any list of users as an aligned, bordered console table. */
    private void printUserTable(List<User> users) {
        String rowFormat = "| %-5s | %-15s | %-12s | %-10s |%n";
        String border = "+" + "-".repeat(7) + "+" + "-".repeat(17)
                + "+" + "-".repeat(14) + "+" + "-".repeat(12) + "+";

        System.out.println(border);
        System.out.printf(rowFormat, "ID", "Username", "Role", "Status");
        System.out.println(border);

        if (users.isEmpty()) {
            System.out.println("| No users to display." + " ".repeat(border.length() - 23) + "|");
        } else {
            for (User u : users) {
                System.out.printf(rowFormat, u.userId(), u.username(), u.role(), u.status());
            }
        }

        System.out.println(border);
        System.out.println("Total: " + users.size());
    }
}
