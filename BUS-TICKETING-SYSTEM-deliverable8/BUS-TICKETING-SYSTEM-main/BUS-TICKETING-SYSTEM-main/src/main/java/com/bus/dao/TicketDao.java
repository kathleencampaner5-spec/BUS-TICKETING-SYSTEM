package com.bus.dao;

import com.bus.model.Ticket;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Ticket booking and lifecycle. Every change is written together with its audit log
 * entry in ONE transaction, so a ticket can never change without a matching log.
 */
public class TicketDao {

    private final ActivityLogDao logDao;

    public TicketDao(ActivityLogDao logDao) {
        this.logDao = logDao;
    }

    /**
     * Core business process: books a seat and logs TICKET_CREATED.
     * @return the new ticket_id, or -1 if the trip is not open or the seat is already taken
     */
    public int bookTicket(int cashierId, int tripId, int passengerId, String seatNumber)
            throws SQLException {
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                double fare;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT fare_amount FROM trips WHERE trip_id = ? AND status = 'SCHEDULED'")) {
                    ps.setInt(1, tripId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            return -1;
                        }
                        fare = rs.getDouble(1);
                    }
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT 1 FROM tickets WHERE trip_id = ? AND seat_number = ? AND status = 'BOOKED'")) {
                    ps.setInt(1, tripId);
                    ps.setString(2, seatNumber);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            con.rollback();
                            return -1; // seat already booked
                        }
                    }
                }

                int ticketId;
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO tickets (trip_id, user_id, seat_number, status) VALUES (?, ?, ?, 'BOOKED')",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, tripId);
                    ps.setInt(2, passengerId);
                    ps.setString(3, seatNumber);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            con.rollback();
                            return -1;
                        }
                        ticketId = keys.getInt(1);
                    }
                }

                logDao.logAction(con, cashierId, "TICKET_CREATED",
                        String.format("Booked Ticket #%d for Passenger [%d], Trip #%d, Seat %s - Fare: P%.2f",
                                ticketId, passengerId, tripId, seatNumber, fare));
                con.commit();
                return ticketId;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /**
     * Lifecycle change: moves a BOOKED ticket to CANCELLED or COMPLETED and logs TICKET_UPDATED.
     * @return true if the ticket was BOOKED and has been updated
     */
    public boolean updateTicketStatus(int actorId, int ticketId, String newStatus)
            throws SQLException {
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                String oldStatus;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT status FROM tickets WHERE ticket_id = ?")) {
                    ps.setInt(1, ticketId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            return false;
                        }
                        oldStatus = rs.getString(1);
                    }
                }
                if (!"BOOKED".equals(oldStatus)) {
                    con.rollback();
                    return false; // only BOOKED tickets can change
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE tickets SET status = ? WHERE ticket_id = ?")) {
                    ps.setString(1, newStatus);
                    ps.setInt(2, ticketId);
                    ps.executeUpdate();
                }

                logDao.logAction(con, actorId, "TICKET_UPDATED",
                        "Updated Ticket #" + ticketId + " status from " + oldStatus + " to " + newStatus);
                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /** @return every ticket, newest first (cashier view) */
    public List<Ticket> getAllTickets() throws SQLException {
        return query("SELECT ticket_id, trip_id, user_id, seat_number, booking_date, status "
                + "FROM tickets ORDER BY ticket_id DESC", null);
    }

    /** @return the tickets that belong to one passenger */
    public List<Ticket> getTicketsByUser(int userId) throws SQLException {
        return query("SELECT ticket_id, trip_id, user_id, seat_number, booking_date, status "
                + "FROM tickets WHERE user_id = ? ORDER BY ticket_id DESC", userId);
    }

    private List<Ticket> query(String sql, Integer userId) throws SQLException {
        List<Ticket> tickets = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (userId != null) {
                ps.setInt(1, userId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tickets.add(new Ticket(
                            rs.getInt("ticket_id"),
                            rs.getInt("trip_id"),
                            rs.getInt("user_id"),
                            rs.getString("seat_number"),
                            rs.getString("booking_date"),
                            rs.getString("status")));
                }
            }
        }
        return tickets;
    }
}
