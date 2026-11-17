package com.bus.ui;

import com.bus.dao.ActivityLogDao;
import com.bus.dao.TicketDao;
import com.bus.dao.TripDao;
import com.bus.dao.UserDao;
import com.bus.model.Role;
import com.bus.model.User;

import java.sql.SQLException;
import java.util.Scanner;

/**
 * Handles the first screen: register, login and routing to the role menu.
 * Contains no SQL; all persistence goes through the DAOs.
 */
public class Authentication {

    private final UserDao userDao;
    private final ActivityLogDao logDao;
    private final TripDao tripDao;
    private final TicketDao ticketDao;
    private final Scanner sc;

    public Authentication(UserDao userDao, ActivityLogDao logDao, TripDao tripDao,
                          TicketDao ticketDao, Scanner sc) {
        this.userDao = userDao;
        this.logDao = logDao;
        this.tripDao = tripDao;
        this.ticketDao = ticketDao;
        this.sc = sc;
    }

    public void start() {
        while (true) {
            System.out.println("\n=== BUS TICKETING SYSTEM ===");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            System.out.print("Choose: ");

            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> register();
                case "2" -> login();
                case "3" -> {
                    System.out.println("Goodbye!");
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private void register() {
        try {
            System.out.print("First name: ");
            String firstName = sc.nextLine().trim();

            System.out.print("Last name: ");
            String lastName = sc.nextLine().trim();

            System.out.print("Phone number: ");
            String phone = sc.nextLine().trim();

            System.out.print("Username: ");
            String username = sc.nextLine().trim();

            System.out.print("Password: ");
            String password = sc.nextLine();

            if (firstName.isBlank() || lastName.isBlank() || phone.isBlank()
                    || username.isBlank() || password.isBlank()) {
                System.out.println("Fields cannot be empty.");
                return;
            }

            Role role = chooseRole();
            if (role == null) {
                System.out.println("Invalid role.");
                return;
            }

            if (userDao.usernameExists(username)) {
                System.out.println("Username already taken.");
                return;
            }

            int userId = userDao.register(firstName, lastName, phone, username, password, role);
            if (userId > 0) {
                logDao.logAction(userId, "USER_REGISTERED",
                        "New " + role + " account registered: " + username + " (ID: " + userId + ")");
                System.out.println("Registration successful!");
                System.out.println("Status: PENDING");
            } else {
                System.out.println("Registration failed.");
            }

        } catch (SQLException e) {
            System.out.println("Registration failed: " + e.getMessage());
        }
    }

    /** Lists every role in the Role enum, so new roles appear automatically. */
    private Role chooseRole() {
        Role[] roles = Role.values();
        for (int i = 0; i < roles.length; i++) {
            System.out.println((i + 1) + ". " + roles[i]);
        }
        System.out.print("Choose role: ");

        try {
            int pick = Integer.parseInt(sc.nextLine().trim());
            return (pick >= 1 && pick <= roles.length) ? roles[pick - 1] : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void login() {
        try {
            System.out.print("Username: ");
            String username = sc.nextLine().trim();

            System.out.print("Password: ");
            String password = sc.nextLine();

            User user = userDao.login(username, password);

            if (user == null) {
                System.out.println("Invalid username or password.");
                return;
            }

            if (!"APPROVED".equalsIgnoreCase(user.status())) {
                System.out.println("Your account status is " + user.status()
                        + ". Please contact the administrator for approval.");
                return;
            }

            logDao.logAction(user.userId(), "LOGIN",
                    "User " + user.username() + " (ID: " + user.userId() + ") logged in.");
            System.out.println("Login successful!");
            routeToMenu(user);
            logout(user);

        } catch (SQLException e) {
            System.out.println("Login failed: " + e.getMessage());
        }
    }

    /** Called when a role menu returns, i.e. the user chose Logout. */
    private void logout(User user) {
        try {
            logDao.logAction(user.userId(), "LOGOUT",
                    "User " + user.username() + " (ID: " + user.userId() + ") logged out.");
        } catch (SQLException e) {
            System.out.println("Note: logout could not be logged: " + e.getMessage());
        }
    }

    private void routeToMenu(User user) {
        switch (user.role()) {
            case ADMIN -> new AdminMenu(user, userDao, logDao, sc).show();
            case CASHIER -> new CashierMenu(user, userDao, tripDao, ticketDao, sc).show();
            case PASSENGER -> new PassengerMenu(user, tripDao, ticketDao, sc).show();
        }
    }
}
