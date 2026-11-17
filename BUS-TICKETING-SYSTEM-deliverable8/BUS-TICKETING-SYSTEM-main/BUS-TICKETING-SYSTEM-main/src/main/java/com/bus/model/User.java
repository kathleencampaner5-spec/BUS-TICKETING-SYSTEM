package com.bus.model;

public record User(
        int userId,
        String firstName,
        String lastName,
        String phoneNumber,
        String username,
        String passwordHash,
        Role role,
        String status
) {
    public String fullName() {
        return firstName + " " + lastName;
    }
}
