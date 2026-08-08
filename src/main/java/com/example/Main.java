package com.example;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Launching MBMS Application ===");

        // Step 3: Execute and Verify
        DatabaseManager.initializeDatabase();

        System.out.println("=== Application System Ready ===");
        
        // Your application business logic (e.g., auth, console menus) goes here
    }
}
