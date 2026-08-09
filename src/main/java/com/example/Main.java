package com.example;

import com.example.dao.EmployeeDAOImpl;
import com.example.domain.SessionContext;
import com.example.service.AccountService;
import com.example.service.AuthService;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Launching MBMS Application ===");
        DatabaseManager.initializeDatabase();
        System.out.println("=== Application System Ready ===\n");

        // 1. Initialize our DAOs and Services
        EmployeeDAOImpl employeeDAO = new EmployeeDAOImpl(DatabaseManager.getDataSource());
        AuthService authService = new AuthService(employeeDAO);
        AccountService accountService = new AccountService(DatabaseManager.getDataSource());
        com.example.dao.TransactionDAOImpl transactionDAO = new com.example.dao.TransactionDAOImpl(DatabaseManager.getDataSource());
        com.example.service.AdminService adminService = new com.example.service.AdminService(DatabaseManager.getDataSource());

        Scanner scanner = new Scanner(System.in);
        SessionContext currentSession = null;

        // ==========================================
        // ONE-TIME FIX: FORCE ADMIN PASSWORD HASH
        // ==========================================
        try (java.sql.Connection conn = DatabaseManager.getDataSource().getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(
                 "UPDATE employees SET password_hash = ? WHERE username = 'admin'")) {
            
            // Generate a mathematically perfect hash for the word "admin"
            String validHash = org.mindrot.jbcrypt.BCrypt.hashpw("admin", org.mindrot.jbcrypt.BCrypt.gensalt());
            ps.setString(1, validHash);
            int rows = ps.executeUpdate();
            
            if (rows > 0) {
                System.out.println("DEBUG: Admin password hash updated successfully!");
            } else {
                System.out.println("DEBUG: No user named 'admin' found! We need to INSERT one.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ==========================================
        // PHASE 1: THE LOGIN LOOP
        // ==========================================
        System.out.println("--- MBMS Secure Login ---");
        while (currentSession == null) {
            System.out.print("Username: ");
            String username = scanner.nextLine();
            
            System.out.print("Password: ");
            String password = scanner.nextLine();

            Optional<SessionContext> sessionOpt = authService.login(username, password);
            
            if (sessionOpt.isPresent()) {
                currentSession = sessionOpt.get();
                System.out.println("\nLogin successful!");
                System.out.println("Welcome, " + currentSession.username() + " [Role: " + currentSession.role() + "]");
            } else {
                System.out.println("Invalid credentials. Please try again.\n");
            }
        }

        // ==========================================
        // PHASE 2: THE INTERACTIVE MENU LOOP
        // ==========================================
        boolean running = true;
        while (running) {
            System.out.println("\n=== MAIN MENU ===");
            System.out.println("1. Transfer Funds");
            System.out.println("2. View Transaction History");
            
            // RBAC in action: Only Admins see this option!
            if ("ADMIN".equals(currentSession.role())) {
                System.out.println("3. System Configuration (Admin Only)");
            }
            
            System.out.println("0. Logout and Exit");
            System.out.print("Select an option: ");
            
            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    System.out.println("\n--- Initiating Transfer ---");
                    
                    try {
                        System.out.print("Enter From Account ID: ");
                        Integer fromAccountId = Integer.parseInt(scanner.nextLine());
                        
                        System.out.print("Enter To Account ID: ");
                        Integer toAccountId = Integer.parseInt(scanner.nextLine());
                        
                        System.out.print("Enter Transfer Amount: ");
                        BigDecimal amount = new BigDecimal(scanner.nextLine()); 

                        // Capture the boolean result!
                        boolean success = accountService.transferFunds(fromAccountId, toAccountId, amount, currentSession.employeeId());
                        
                        if (success) {
                            System.out.println("Transfer completed successfully!");
                            System.out.println("Moved $" + amount + " from Account " + fromAccountId + " to Account " + toAccountId);
                        } else {
                            System.out.println("Transfer failed! Please check account balances and try again.");
                        }
                    } catch (NumberFormatException e) {
                        // This prevents the app from crashing if the user types "abc" instead of a number!
                        System.out.println("Invalid input. Please enter valid numbers.");
                    }
                    break;
                
                case "2":
                    System.out.println("\n--- Transaction History ---");
                    try {
                        System.out.print("Enter Account ID: ");
                        Integer accId = Integer.parseInt(scanner.nextLine());
                        
                        java.util.List<com.example.domain.Transaction> history = transactionDAO.getTransactionsForAccount(accId);
                        
                        if (history.isEmpty()) {
                            System.out.println("No transactions found for Account " + accId);
                        } else {
                            System.out.println("\nDate                | Ref Number                           | Type     | Amount   | Status");
                            System.out.println("---------------------------------------------------------------------------------------------");
                            for (com.example.domain.Transaction txn : history) {
                                String action;
                                String details;
                                String sign;
                                
                                if (txn.getFromAccountId().equals(accId)) {
                                    action = "DEBIT";
                                    details = "To Acc " + txn.getToAccountId();
                                    sign = "-"; // Money leaving
                                } else {
                                    action = "CREDIT";
                                    details = "From Acc " + txn.getFromAccountId();
                                    sign = "+"; // Money entering
                                }
                                // ============================================
                                System.out.printf("%-19s | %-36s | %-6s | %-20s | %s$%-8.2f | %s%n", 
                                    txn.getCreatedAt().toString().substring(0, 19), 
                                    txn.getReferenceNumber(),
                                    action,
                                    details,
                                    sign,
                                    txn.getAmount(),
                                    txn.getStatus()
                                );
                            }
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input. Please enter a valid Account ID number.");
                    }
                    break;

                case "3":
                    if ("ADMIN".equals(currentSession.role())) {
                        System.out.println("\n---Admin System Configuration ---");
                        System.out.println("1. Hire New Employee");
                        System.out.println("2. Run Concurrency Stress Test");
                        System.out.println("0. Back to Main Menu");
                        System.out.print("Select an option: ");
                        
                        String adminChoice = scanner.nextLine();
                        
                        if ("1".equals(adminChoice)) {
                            System.out.println("\n--- Hire New Employee ---");
                            try {
                                System.out.print("Enter Branch ID (e.g., 1 or 2): ");
                                Integer branchId = Integer.parseInt(scanner.nextLine());
                                
                                System.out.print("Enter New Username: ");
                                String newUsername = scanner.nextLine();
                                
                                System.out.print("Enter Temporary Password: ");
                                String tempPassword = scanner.nextLine();
                                
                                System.out.print("Enter Role (ADMIN / TELLER): ");
                                String newRole = scanner.nextLine().toUpperCase(); // Force uppercase to match DB
                                
                                boolean success = adminService.createEmployee(newUsername, tempPassword, newRole, branchId);
                                
                                if (success) {
                                    System.out.println("Successfully hired " + newUsername + " as a " + newRole + "!");
                                } else {
                                    System.out.println("Failed to create employee. Username might already exist.");
                                }
                            } catch (NumberFormatException e) {
                                System.out.println("Invalid input. Branch ID must be a number.");
                            }
                        } else if ("2".equals(adminChoice)) {
                            // Run the stress test!
                            ConcurrencyTest.runStressTest(accountService);
                        } else if (!"0".equals(adminChoice)) {
                            System.out.println("Invalid Admin Option.");
                        }
                    } else {
                        // Just in case a Teller somehow triggers case 3
                        System.out.println("\nACCESS DENIED. You do not have Admin privileges.");
                    }
                    break;

                case "0":
                    System.out.println("\nLogging out... Goodbye!");
                    running = false;
                    break;
                    
                default:
                    System.out.println("\nInvalid option. Please try again.");
            }
        }
        
        scanner.close();
    }
}