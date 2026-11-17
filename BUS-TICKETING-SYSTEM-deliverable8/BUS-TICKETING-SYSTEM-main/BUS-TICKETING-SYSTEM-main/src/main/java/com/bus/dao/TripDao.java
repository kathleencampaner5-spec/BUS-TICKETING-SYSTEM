package com.bus.dao;

import com.bus.model.TripInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TripDao {

    /** @return every SCHEDULED trip with its bus and route details */
    public List<TripInfo> getScheduledTrips() throws SQLException {
        String sql = """
                SELECT t.trip_id, b.bus_number, r.origin, r.destination,
                       t.departure_datetime, t.fare_amount
                FROM trips t
                JOIN buses  b ON b.bus_id   = t.bus_id
                JOIN routes r ON r.route_id = t.route_id
                WHERE t.status = ?
                ORDER BY t.departure_datetime
                """;

        List<TripInfo> trips = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "SCHEDULED");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    trips.add(new TripInfo(
                            rs.getInt("trip_id"),
                            rs.getString("bus_number"),
                            rs.getString("origin"),
                            rs.getString("destination"),
                            rs.getString("departure_datetime"),
                            rs.getDouble("fare_amount")));
                }
            }
        }
        return trips;
    }
}
