package com.bus.model;

public record Ticket(
        int ticketId,
        int tripId,
        int userId,
        String seatNumber,
        String bookingDate,
        String status
) {
}
