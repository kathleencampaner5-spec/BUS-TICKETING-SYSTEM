package com.bus.ui;

import com.bus.dao.TicketDao;
import com.bus.dao.TripDao;
import com.bus.dao.UserDao;
import com.bus.model.Role;
import com.bus.model.User;

import java.sql.SQLException;
import java.util.Scanner;

public class CashierMenu {

    private final User user;
    private final UserDao userDao;
    private final TripDao tripDao;
    private final TicketDao ticketDao;
    private final Scanner sc;

    public CashierMenu(User user, UserDao userDao, TripDao tripDao, TicketDao ticketDao, Scanner sc) {
        this.user = user;
        this.userDao = userDao;
        this.tripDao = tripDao;
        this.ticketDao = ticketDao;
        this.sc = sc;
    }

    public void show() {
        System.out.println("\nWelcome, " + user.fullName() + " (CASHIER)");
        System.out.println("Status: " + user.status());

        while (true) {
            System.out.println("\n=== CASHIER MENU ===");
            System.out.println("1. View Account");
            System.out.println("2. View Scheduled Trips");
            System.out.println("3. Book Ticket");
            System.out.println("4. View All Tickets");
            System.out.println("5. Update Ticket Status");
            System.out.println("6. Logout");
            System.out.print("Choose: ");

            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> viewAccount();
                case "2" -> viewTrips();
                case "3" -> bookTicket();
                case "4" -> viewTickets();
                case "5" -> updateTicketStatus();
                case "6" -> {
                    System.out.println("Logged out successfully.");
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private void viewAccount() {
        System.out.println("Name: " + user.fullName());
        System.out.println("Username: " + user.username());
        System.out.println("Phone: " + user.phoneNumber());
        System.out.println("Role: " + user.role());
        System.out.println("Status: " + user.status());
    }

    private void viewTrips() {
        try {
            System.out.println("\n--- SCHEDULED TRIPS ---");
            TicketViews.printTrips(tripDao.getScheduledTrips());
        } catch (SQLException e) {
            System.out.println("Could not load trips: " + e.getMessage());
        }
    }

    private void viewTickets() {
        try {
            System.out.println("\n--- ALL TICKETS ---");
            TicketViews.printTickets(ticketDao.getAllTickets());
        } catch (SQLException e) {
            System.out.println("Could not load tickets: " + e.getMessage());
        }
    }

    /** Books a seat for an approved passenger. Enter 0 at a number prompt to cancel. */
    private void bookTicket() {
        try {
            viewTrips();
            int tripId = TicketViews.readInt(sc, "Enter Trip ID (0 to cancel): ");
            if (tripId == 0) {
                System.out.println("Booking cancelled.");
                return;
            }

            User passenger;
            while (true) {
                int passengerId = TicketViews.readInt(sc, "Enter Passenger User ID (0 to cancel): ");
                if (passengerId == 0) {
                    System.out.println("Booking cancelled.");
                    return;
                }
                passenger = userDao.getUserById(passengerId);
                if (passenger == null || passenger.role() != Role.PASSENGER
                        || !"APPROVED".equals(passenger.status())) {
                    System.out.println("User ID " + passengerId + " is not an approved passenger. Try again.");
                    continue;
                }
                break;
            }

            System.out.print("Enter seat number (e.g. 12A): ");
            String seat = sc.nextLine().trim().toUpperCase();
            if (seat.isBlank()) {
                System.out.println("Seat number cannot be empty.");
                return;
            }

            int ticketId = ticketDao.bookTicket(user.userId(), tripId, passenger.userId(), seat);
            if (ticketId > 0) {
                System.out.println("SUCCESS: Ticket #" + ticketId + " booked for "
                        + passenger.username() + ", Seat " + seat + ".");
            } else {
                System.out.println("Booking failed. The trip is not open or the seat is already booked.");
            }
        } catch (SQLException e) {
            System.out.println("Booking failed: " + e.getMessage());
        }
    }

    private void updateTicketStatus() {
        try {
            viewTickets();
            int ticketId = TicketViews.readInt(sc, "Enter Ticket ID (0 to cancel): ");
            if (ticketId == 0) {
                System.out.println("Update cancelled.");
                return;
            }

            String newStatus;
            while (true) {
                System.out.print("New status (CANCELLED, COMPLETED) (0 to cancel): ");
                newStatus = sc.nextLine().trim().toUpperCase();
                if (newStatus.equals("0")) {
                    System.out.println("Update cancelled.");
                    return;
                }
                if (newStatus.equals("CANCELLED") || newStatus.equals("COMPLETED")) {
                    break;
                }
                System.out.println("Invalid status. Please enter CANCELLED or COMPLETED.");
            }

            if (ticketDao.updateTicketStatus(user.userId(), ticketId, newStatus)) {
                System.out.println("SUCCESS: Ticket #" + ticketId + " is now " + newStatus + ".");
            } else {
                System.out.println("Update failed. The ticket does not exist or is no longer BOOKED.");
            }
        } catch (SQLException e) {
            System.out.println("Update failed: " + e.getMessage());
        }
    }
}
