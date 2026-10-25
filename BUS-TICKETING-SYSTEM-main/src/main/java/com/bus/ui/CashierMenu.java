package com.bus.ui;

import com.bus.model.User;

import java.util.Scanner;

public class CashierMenu {

    private final User user;
    private final Scanner sc;

    public CashierMenu(User user, Scanner sc) {
        this.user = user;
        this.sc = sc;
    }

    public void show() {
        System.out.println("\nWelcome, " + user.fullName() + " (CASHIER)");
        System.out.println("Status: " + user.status());

        while (true) {
            System.out.println("\n=== CASHIER MENU ===");
            System.out.println("1. View Account");
            System.out.println("2. Logout");
            System.out.print("Choose: ");

            String choice = sc.nextLine().trim();

            if (choice.equals("1")) {
                System.out.println("Name: " + user.fullName());
                System.out.println("Username: " + user.username());
                System.out.println("Phone: " + user.phoneNumber());
                System.out.println("Role: " + user.role());
                System.out.println("Status: " + user.status());
            } else if (choice.equals("2")) {
                System.out.println("Logged out successfully.");
                return;
            } else {
                System.out.println("Invalid choice.");
            }
        }
    }
}
