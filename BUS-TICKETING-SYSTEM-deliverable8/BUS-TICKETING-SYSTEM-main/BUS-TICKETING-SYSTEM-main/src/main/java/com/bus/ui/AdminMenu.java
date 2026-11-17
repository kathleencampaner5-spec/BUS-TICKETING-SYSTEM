package com.bus.ui;

import com.bus.dao.ActivityLogDao;
import com.bus.dao.UserDao;
import com.bus.model.ActivityLog;
import com.bus.model.User;
import com.bus.model.Role;

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
            System.out.println("5. Update User Role and Status");
            System.out.println("6. View System Audit Logs");
            System.out.println("7. Logout");
            System.out.print("Choose: ");

            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> viewAllUsers();
                case "2" -> viewPendingUsers();
                case "3" -> approveUserAccount();
                case "4" -> viewMyAccount();
                case "5" -> updateUserRoleAndStatus();
                case "6" -> auditLogMenu();
                case "7" -> {
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

                if (userDao.approveUser(user.userId(), targetId)) {
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


    /**
     * Lets an admin change another user's role and status.
     * Enter 0 at any prompt to cancel. An admin cannot edit their own account.
     */
    private void updateUserRoleAndStatus() {
        try {
            List<User> users = userDao.getAllUsers();

            System.out.println("\n--- UPDATE USER ROLE AND STATUS ---");
            printUserTable(users);
            if (users.isEmpty()) {
                return;
            }

            // Step 1: pick the target user (re-prompt until valid, 0 cancels)
            User target;
            while (true) {
                int targetUserId = readIntInput("Enter Target User ID (0 to cancel): ");
                if (targetUserId == 0) {
                    System.out.println("Update cancelled.");
                    return;
                }
                if (targetUserId == user.userId()) {
                    System.out.println("WARNING: Modifying the currently active admin account is restricted.");
                    continue;
                }
                target = findById(users, targetUserId);
                if (target == null) {
                    System.out.println("User ID " + targetUserId + " was not found. Try again.");
                    continue;
                }
                break;
            }

            // Step 2: new role
            String newRole = readValidRole();
            if (newRole == null) {
                System.out.println("Update cancelled.");
                return;
            }

            // Step 3: new status
            String newStatus = readValidStatus();
            if (newStatus == null) {
                System.out.println("Update cancelled.");
                return;
            }

            if (userDao.updateUserRoleAndStatus(user.userId(), target.userId(), newRole, newStatus)) {
                System.out.println("SUCCESS: User ID " + target.userId() + " ('" + target.username()
                        + "') updated. New Role: " + newRole + ", New Status: " + newStatus + ".");
            } else {
                System.out.println("Update failed. The user may no longer exist.");
            }
        } catch (SQLException e) {
            System.out.println("User update failed: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------------
    // Audit log interface (display only - all SQL is in ActivityLogDao)
    // ---------------------------------------------------------------------

    private static final String LOG_ROW_FORMAT = "%-5s | %-8s | %-18s | %-35s | %-20s%n";
    private static final int DESCRIPTION_WIDTH = 35;

    private void auditLogMenu() {
        while (true) {
            System.out.println("\n=== SYSTEM AUDIT LOGS ===");
            System.out.println("1. View All System Logs");
            System.out.println("2. Filter Logs by User ID");
            System.out.println("3. Clear All Logs");
            System.out.println("0. Back");
            System.out.print("Choose: ");

            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> viewAllLogs();
                case "2" -> filterLogsByUserId();
                case "3" -> clearLogs();
                case "0" -> {
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private void viewAllLogs() {
        try {
            System.out.println("\n--- ALL SYSTEM LOGS ---");
            printLogTable(logDao.getAllLogs());
        } catch (SQLException e) {
            System.out.println("Could not load logs: " + e.getMessage());
        }
    }

    private void filterLogsByUserId() {
        int userId = readIntInput("Enter User ID to filter (0 to cancel): ");
        if (userId == 0) {
            System.out.println("Filter cancelled.");
            return;
        }
        try {
            System.out.println("\n--- LOGS FOR USER ID " + userId + " ---");
            printLogTable(logDao.getLogsByUserId(userId));
        } catch (SQLException e) {
            System.out.println("Could not load logs: " + e.getMessage());
        }
    }

    /** Deleting history is irreversible, so the admin must type CLEAR to confirm. */
    private void clearLogs() {
        System.out.println("WARNING: This permanently deletes ALL historical logs.");
        System.out.print("Type CLEAR to confirm (anything else cancels): ");
        if (!"CLEAR".equals(sc.nextLine().trim())) {
            System.out.println("Clear cancelled. No logs were deleted.");
            return;
        }
        try {
            int deleted = logDao.clearLogs(user.userId());
            System.out.println("SUCCESS: " + deleted + " log entries deleted. "
                    + "This action has been recorded as LOGS_CLEARED.");
        } catch (SQLException e) {
            System.out.println("Could not clear logs: " + e.getMessage());
        }
    }

    /** Aligned table; long descriptions wrap onto extra lines instead of breaking the columns. */
    private void printLogTable(List<ActivityLog> logs) {
        String header = String.format(LOG_ROW_FORMAT.replace("%n", ""),
                "ID", "User ID", "Action Type", "Description", "Timestamp");
        String line = "-".repeat(header.length());

        System.out.println(line);
        System.out.println(header);
        System.out.println(line);

        if (logs.isEmpty()) {
            System.out.println("No logs to display.");
        } else {
            for (ActivityLog log : logs) {
                List<String> parts = wrap(log.description() == null ? "" : log.description());
                System.out.printf(LOG_ROW_FORMAT, log.logId(), log.userId(), log.actionType(),
                        parts.get(0), log.timestamp());
                for (int i = 1; i < parts.size(); i++) {
                    System.out.printf(LOG_ROW_FORMAT, "", "", "", parts.get(i), "");
                }
            }
        }

        System.out.println(line);
        System.out.println("Total: " + logs.size());
    }

    /** Splits text into pieces of at most DESCRIPTION_WIDTH characters, breaking at spaces. */
    private List<String> wrap(String text) {
        List<String> parts = new java.util.ArrayList<>();
        String rest = text.trim();
        while (rest.length() > DESCRIPTION_WIDTH) {
            int cut = rest.lastIndexOf(' ', DESCRIPTION_WIDTH);
            if (cut <= 0) {
                cut = DESCRIPTION_WIDTH;
            }
            parts.add(rest.substring(0, cut).trim());
            rest = rest.substring(cut).trim();
        }
        parts.add(rest);
        return parts;
    }

    /** Reads a non-negative integer safely, re-asking on letters/symbols. Zero is the cancel value. */
    private int readIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value >= 0) {
                    return value;
                }
            } catch (NumberFormatException e) {
                // fall through to the message below
            }
            System.out.println("Invalid input. Please enter a whole number (0 to cancel).");
        }
    }

    /** Returns null when the user enters 0 to cancel. */
    private String readValidRole() {
        while (true) {
            System.out.print("Enter new role " + validRoles() + " (0 to cancel): ");
            String input = sc.nextLine().trim().toUpperCase();
            if ("0".equals(input)) {
                return null;
            }
            try {
                return Role.valueOf(input).name();
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid role. Allowed roles: " + validRoles());
            }
        }
    }

    /** Returns null when the user enters 0 to cancel. */
    private String readValidStatus() {
        while (true) {
            System.out.print("Enter new status (APPROVED, PENDING) (0 to cancel): ");
            String input = sc.nextLine().trim().toUpperCase();
            if ("0".equals(input)) {
                return null;
            }
            if ("APPROVED".equals(input) || "PENDING".equals(input)) {
                return input;
            }
            System.out.println("Invalid status. Please enter APPROVED or PENDING.");
        }
    }

    private String validRoles() {
        StringBuilder roles = new StringBuilder();
        for (Role role : Role.values()) {
            if (roles.length() > 0) roles.append(", ");
            roles.append(role.name());
        }
        return "(" + roles + ")";
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
