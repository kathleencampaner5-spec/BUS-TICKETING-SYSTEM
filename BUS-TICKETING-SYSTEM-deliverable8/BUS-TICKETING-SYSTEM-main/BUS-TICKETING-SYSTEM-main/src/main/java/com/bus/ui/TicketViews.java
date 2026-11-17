package com.bus.ui;

import com.bus.model.Ticket;
import com.bus.model.TripInfo;

import java.util.List;

/** Console tables shared by the Cashier and Passenger menus. Display only - no SQL. */
final class TicketViews {

    private TicketViews() {
    }

    static void printTrips(List<TripInfo> trips) {
        String row = "| %-7s | %-9s | %-12s | %-12s | %-16s | %9s |%n";
        String line = "+" + "-".repeat(9) + "+" + "-".repeat(11) + "+" + "-".repeat(14)
                + "+" + "-".repeat(14) + "+" + "-".repeat(18) + "+" + "-".repeat(11) + "+";
        System.out.println(line);
        System.out.printf(row, "Trip ID", "Bus", "From", "To", "Departure", "Fare");
        System.out.println(line);
        if (trips.isEmpty()) {
            System.out.println("No scheduled trips.");
        }
        for (TripInfo t : trips) {
            String departure = t.departure().length() > 16 ? t.departure().substring(0, 16) : t.departure();
            System.out.printf(row, t.tripId(), t.busNumber(), t.origin(), t.destination(),
                    departure, String.format("P%.2f", t.fare()));
        }
        System.out.println(line);
    }

    static void printTickets(List<Ticket> tickets) {
        String row = "| %-9s | %-7s | %-12s | %-6s | %-19s | %-10s |%n";
        String line = "+" + "-".repeat(11) + "+" + "-".repeat(9) + "+" + "-".repeat(14)
                + "+" + "-".repeat(8) + "+" + "-".repeat(21) + "+" + "-".repeat(12) + "+";
        System.out.println(line);
        System.out.printf(row, "Ticket ID", "Trip ID", "Passenger ID", "Seat", "Booked On", "Status");
        System.out.println(line);
        if (tickets.isEmpty()) {
            System.out.println("No tickets to display.");
        }
        for (Ticket t : tickets) {
            System.out.printf(row, t.ticketId(), t.tripId(), t.userId(), t.seatNumber(),
                    t.bookingDate(), t.status());
        }
        System.out.println(line);
        System.out.println("Total: " + tickets.size());
    }

    /** Re-prompts until the user types a whole number >= 0. Zero means cancel. */
    static int readInt(java.util.Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                int value = Integer.parseInt(sc.nextLine().trim());
                if (value >= 0) {
                    return value;
                }
            } catch (NumberFormatException e) {
                // fall through
            }
            System.out.println("Invalid input. Please enter a whole number (0 to cancel).");
        }
    }
}
