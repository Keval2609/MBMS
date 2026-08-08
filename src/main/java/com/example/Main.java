package com.example;

import java.math.BigDecimal;

import com.example.service.AccountService;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Launching MBMS Application ===");

        // Step 3: Execute and Verify
        DatabaseManager.initializeDatabase();

        System.out.println("=== Application System Ready ===");
        
        // Your application business logic (e.g., auth, console menus) goes here
        System.out.println("Initiating Transfer of $50.00 from Account 3 to Account 4...");
        
        try (java.sql.Connection c = DatabaseManager.getDataSource().getConnection();
            java.sql.PreparedStatement ps = c.prepareStatement("SELECT id FROM accounts");
            java.sql.ResultSet rs = ps.executeQuery()) {
            System.out.print("Accounts found by Java: ");
            while(rs.next()) {
                System.out.print(rs.getInt(1) + " ");
            }
            System.out.println();
        } catch (Exception e) {
            e.printStackTrace();
        }

        AccountService accountService = new AccountService(DatabaseManager.getDataSource());
        boolean success = accountService.transferFunds(
            3, 
            4, 
            new BigDecimal("50.00"), 
            1
        );

        if (success) {
            System.out.println("✅ TRANSFER SUCCESSFUL!");
        } else {
            System.out.println("❌ TRANSFER FAILED!");
        }
    }
}
