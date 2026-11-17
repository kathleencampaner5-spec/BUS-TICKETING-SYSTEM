package com.bus.model;

/** A trip joined with its bus and route, ready for display. */
public record TripInfo(
        int tripId,
        String busNumber,
        String origin,
        String destination,
        String departure,
        double fare
) {
}
