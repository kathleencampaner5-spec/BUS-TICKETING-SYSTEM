package com.bus.ui;

import com.bus.dao.TicketDao;
import com.bus.dao.TripDao;
import com.bus.model.User;

import java.sql.SQLException;
import java.util.Scanner;

public class PassengerMenu {

    private final User user;
    private final TripDao tripDao;
    private final TicketDao ticketDao;
    private final Scanner sc;

    public PassengerMenu(User user, TripDao tripDao, TicketDao ticketDao, Scanner sc) {
        this.user = user;
        this.tripDao = tripDao;
        this.ticketDao = ticketDao;
        this.sc = sc;
    }

    public void show() {
        System.out.println("\nWelcome, " + user.fullName() + " (PASSENGER)");
        System.out.println("Status: " + user.status());

        while (true) {
            System.out.println("\n=== PASSENGER MENU ===");
            System.out.println("1. View Account");
            System.out.println("2. View Scheduled Trips");
            System.out.println("3. View My Tickets");
            System.out.println("4. Logout");
            System.out.print("Choose: ");

            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> {
                    System.out.println("Name: " + user.fullName());
                    System.out.println("Username: " + user.username());
                    System.out.println("Phone: " + user.phoneNumber());
                    System.out.println("Role: " + user.role());
                    System.out.println("Status: " + user.status());
                }
                case "2" -> {
                    try {
                        System.out.println("\n--- SCHEDULED TRIPS ---");
                        TicketViews.printTrips(tripDao.getScheduledTrips());
                    } catch (SQLException e) {
                        System.out.println("Could not load trips: " + e.getMessage());
                    }
                }
                case "3" -> {
                    try {
                        System.out.println("\n--- MY TICKETS ---");
                        TicketViews.printTickets(ticketDao.getTicketsByUser(user.userId()));
                    } catch (SQLException e) {
                        System.out.println("Could not load tickets: " + e.getMessage());
                    }
                }
                case "4" -> {
                    System.out.println("Logged out successfully.");
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }
}
