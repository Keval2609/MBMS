package com.example;

import com.example.dao.AccountDAO;
import com.example.dao.AccountDAOImpl;
import com.example.dao.CustomerDAO;
import com.example.dao.CustomerDAOImpl;
import com.example.dao.EmployeeDAO;
import com.example.dao.EmployeeDAOImpl;
import com.example.dao.TransactionDAO;
import com.example.dao.TransactionDAOImpl;
import com.example.dao.TransactionManager;
import com.example.domain.Account;
import com.example.domain.AccountType;
import com.example.domain.Customer;
import com.example.domain.Role;
import com.example.domain.SessionContext;
import com.example.domain.Transaction;
import com.example.exception.BankException;
import com.example.exception.DataAccessException;
import com.example.service.AccountService;
import com.example.service.AdminService;
import com.example.service.AuthService;
import com.example.service.CustomerService;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Launching MBMS Application ===");
        DatabaseManager.initializeDatabase();
        System.out.println("=== Application System Ready ===\n");

        // 1. Initialize DataSource and DAOs
        DataSource dataSource = DatabaseManager.getDataSource();
        TransactionManager transactionManager = new TransactionManager(dataSource);
        AccountDAO accountDAO = new AccountDAOImpl(dataSource);
        TransactionDAO transactionDAO = new TransactionDAOImpl(dataSource);
        EmployeeDAO employeeDAO = new EmployeeDAOImpl(dataSource);
        CustomerDAO customerDAO = new CustomerDAOImpl(dataSource);

        // 2. Initialize Services (Pure business rules, no direct DataSource or raw SQL)
        AuthService authService = new AuthService(employeeDAO);
        AccountService accountService = new AccountService(accountDAO, transactionDAO, transactionManager, customerDAO);
        CustomerService customerService = new CustomerService(customerDAO);
        AdminService adminService = new AdminService(employeeDAO);

        Scanner scanner = new Scanner(System.in);
        SessionContext currentSession = null;

        // ==========================================
        // PHASE 1: THE LOGIN LOOP
        // ==========================================
        System.out.println("--- MBMS Secure Login ---");
        while (currentSession == null) {
            System.out.print("Username: ");
            String username = scanner.nextLine();
            
            System.out.print("Password: ");
            String password = scanner.nextLine();

            try {
                Optional<SessionContext> sessionOpt = authService.login(username, password);
                if (sessionOpt.isPresent()) {
                    currentSession = sessionOpt.get();
                    System.out.println("\nLogin successful!");
                    System.out.println("Welcome, " + currentSession.username() + " [Role: " + currentSession.role() + "]");
                } else {
                    System.out.println("Invalid credentials. Please try again.\n");
                }
            } catch (DataAccessException e) {
                System.err.println("Database connectivity error: " + e.getMessage() + "\n");
            }
        }

        // ==========================================
        // PHASE 2: THE INTERACTIVE MENU LOOP
        // ==========================================
        boolean running = true;
        while (running) {
            System.out.println("\n=== MAIN MENU ===");
            System.out.println("1. Transfer Funds");
            System.out.println("2. Deposit Funds");
            System.out.println("3. Withdraw Funds");
            System.out.println("4. View Transaction History");
            System.out.println("5. Account & Customer Management");
            
            if (Role.ADMIN.matches(currentSession.role())) {
                System.out.println("6. System Configuration (Admin Only)");
            }
            
            System.out.println("0. Logout and Exit");
            System.out.print("Select an option: ");
            
            String choice = scanner.nextLine().trim();

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

                        Transaction txn = accountService.transferFunds(currentSession, fromAccountId, toAccountId, amount);
                        System.out.println("Transfer completed successfully! Ref: " + txn.getReferenceNumber());
                        System.out.println("Moved $" + txn.getAmount() + " from Account " + fromAccountId + " to Account " + toAccountId);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input. Please enter valid numeric values.");
                    } catch (BankException e) {
                        System.out.println("Transfer failed: " + e.getMessage());
                    } catch (Exception e) {
                        System.out.println("Unexpected system error: " + e.getMessage());
                    }
                    break;

                case "2":
                    System.out.println("\n--- Deposit Funds ---");
                    try {
                        System.out.print("Enter Account ID: ");
                        Integer accId = Integer.parseInt(scanner.nextLine());

                        System.out.print("Enter Deposit Amount: ");
                        BigDecimal amount = new BigDecimal(scanner.nextLine());

                        Transaction txn = accountService.deposit(currentSession, accId, amount);
                        System.out.println("Deposit completed successfully! Ref: " + txn.getReferenceNumber());
                        System.out.println("Deposited $" + txn.getAmount() + " into Account " + accId);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input. Please enter valid numeric values.");
                    } catch (BankException e) {
                        System.out.println("Deposit failed: " + e.getMessage());
                    } catch (Exception e) {
                        System.out.println("Unexpected error: " + e.getMessage());
                    }
                    break;

                case "3":
                    System.out.println("\n--- Withdraw Funds ---");
                    try {
                        System.out.print("Enter Account ID: ");
                        Integer accId = Integer.parseInt(scanner.nextLine());

                        System.out.print("Enter Withdrawal Amount: ");
                        BigDecimal amount = new BigDecimal(scanner.nextLine());

                        Transaction txn = accountService.withdraw(currentSession, accId, amount);
                        System.out.println("Withdrawal completed successfully! Ref: " + txn.getReferenceNumber());
                        System.out.println("Withdrew $" + txn.getAmount() + " from Account " + accId);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input. Please enter valid numeric values.");
                    } catch (BankException e) {
                        System.out.println("Withdrawal failed: " + e.getMessage());
                    } catch (Exception e) {
                        System.out.println("Unexpected error: " + e.getMessage());
                    }
                    break;
                
                case "4":
                    System.out.println("\n--- Transaction History ---");
                    try {
                        System.out.print("Enter Account ID: ");
                        Integer accId = Integer.parseInt(scanner.nextLine());
                        
                        List<Transaction> history = transactionDAO.getTransactionsForAccount(accId);
                        
                        if (history.isEmpty()) {
                            System.out.println("No transactions found for Account " + accId);
                        } else {
                            System.out.println("\nDate                | Ref Number                           | Type       | Details              | Amount     | Status");
                            System.out.println("-----------------------------------------------------------------------------------------------------------------");
                            for (Transaction txn : history) {
                                String details;
                                String sign = "";
                                if (accId.equals(txn.getFromAccountId()) && txn.getToAccountId() != null) {
                                    details = "To Acc " + txn.getToAccountId();
                                    sign = "-";
                                } else if (accId.equals(txn.getToAccountId()) && txn.getFromAccountId() != null) {
                                    details = "From Acc " + txn.getFromAccountId();
                                    sign = "+";
                                } else if (txn.getFromAccountId() == null) {
                                    details = "Direct Deposit";
                                    sign = "+";
                                } else {
                                    details = "Direct Withdrawal";
                                    sign = "-";
                                }

                                String dateStr = txn.getCreatedAt() != null ? txn.getCreatedAt().toString().substring(0, Math.min(19, txn.getCreatedAt().toString().length())) : "N/A";
                                System.out.printf("%-19s | %-36s | %-10s | %-20s | %s$%-9.2f | %s%n", 
                                    dateStr, 
                                    txn.getReferenceNumber(),
                                    txn.getType(),
                                    details,
                                    sign,
                                    txn.getAmount(),
                                    txn.getStatus()
                                );
                            }
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input. Please enter a valid Account ID number.");
                    } catch (Exception e) {
                        System.out.println("Error reading transaction history: " + e.getMessage());
                    }
                    break;

                case "5":
                    System.out.println("\n--- Account & Customer Management ---");
                    System.out.println("1. Open New Account");
                    System.out.println("2. Register New Customer");
                    System.out.println("3. List Registered Customers");
                    System.out.println("0. Back to Main Menu");
                    System.out.print("Select an option: ");
                    
                    String mgmtChoice = scanner.nextLine().trim();
                    if ("1".equals(mgmtChoice)) {
                        System.out.println("\n--- Open New Account ---");
                        try {
                            System.out.print("Enter Customer ID (or 0 to register a new customer first): ");
                            int custIdInput = Integer.parseInt(scanner.nextLine().trim());
                            Integer customerId;
                            if (custIdInput == 0) {
                                Customer newCust = registerCustomer(scanner, customerService, currentSession);
                                if (newCust == null) {
                                    break;
                                }
                                customerId = newCust.getId();
                            } else {
                                customerId = custIdInput;
                            }

                            int defaultBranch = currentSession.branchId() != null ? currentSession.branchId() : 1;
                            System.out.print("Enter Branch ID [Press Enter for Branch " + defaultBranch + "]: ");
                            String branchInput = scanner.nextLine().trim();
                            Integer branchId = branchInput.isEmpty() ? defaultBranch : Integer.parseInt(branchInput);

                            System.out.print("Select Account Type (1. SAVINGS, 2. CURRENT) [Default: 1]: ");
                            String typeInput = scanner.nextLine().trim();
                            AccountType accountType = "2".equals(typeInput) ? AccountType.CURRENT : AccountType.SAVINGS;

                            System.out.print("Enter Initial Deposit Amount [Default: 0.00]: ");
                            String depositInput = scanner.nextLine().trim();
                            BigDecimal initialDeposit = depositInput.isEmpty() ? BigDecimal.ZERO : new BigDecimal(depositInput);

                            Account newAcc = accountService.openAccount(currentSession, customerId, branchId, accountType, initialDeposit);
                            System.out.println("\nAccount opened successfully!");
                            System.out.println("Account ID:      " + newAcc.getId());
                            System.out.println("Account Number:  " + newAcc.getAccountNumber());
                            System.out.println("Customer ID:     " + newAcc.getCustomerId());
                            System.out.println("Branch ID:       " + newAcc.getBranchId());
                            System.out.println("Account Type:    " + newAcc.getAccountType());
                            System.out.println("Initial Balance: $" + newAcc.getBalance());
                            System.out.println("Status:          " + newAcc.getStatus());
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid numeric input: " + e.getMessage());
                        } catch (BankException e) {
                            System.out.println("Account opening failed: " + e.getMessage());
                        } catch (Exception e) {
                            System.out.println("Unexpected error: " + e.getMessage());
                        }
                    } else if ("2".equals(mgmtChoice)) {
                        registerCustomer(scanner, customerService, currentSession);
                    } else if ("3".equals(mgmtChoice)) {
                        System.out.println("\n--- Registered Customers ---");
                        try {
                            List<Customer> customers = customerService.listCustomers(currentSession);
                            if (customers.isEmpty()) {
                                System.out.println("No customers registered yet.");
                            } else {
                                System.out.printf("%-5s | %-25s | %-30s | %-15s%n", "ID", "Name", "Email", "Phone");
                                System.out.println("----------------------------------------------------------------------------------");
                                for (Customer c : customers) {
                                    System.out.printf("%-5d | %-25s | %-30s | %-15s%n",
                                            c.getId(), c.getFullName(), c.getEmail(), c.getPhoneNumber());
                                }
                            }
                        } catch (BankException e) {
                            System.out.println("Failed to list customers: " + e.getMessage());
                        } catch (Exception e) {
                            System.out.println("Error reading customers: " + e.getMessage());
                        }
                    }
                    break;

                case "6":
                    if (Role.ADMIN.matches(currentSession.role())) {
                        System.out.println("\n--- Admin System Configuration ---");
                        System.out.println("1. Hire New Employee");
                        System.out.println("0. Back to Main Menu");
                        System.out.print("Select an option: ");
                        
                        String adminChoice = scanner.nextLine().trim();
                        if ("1".equals(adminChoice)) {
                            System.out.println("\n--- Hire New Employee ---");
                            try {
                                System.out.print("Enter Branch ID: ");
                                Integer branchId = Integer.parseInt(scanner.nextLine());
                                
                                System.out.print("Enter New Username: ");
                                String newUsername = scanner.nextLine();
                                
                                System.out.print("Enter Temporary Password: ");
                                String tempPassword = scanner.nextLine();
                                
                                System.out.print("Enter Role (ADMIN / MANAGER / TELLER): ");
                                Role newRole = Role.fromString(scanner.nextLine());
                                
                                adminService.createEmployee(currentSession, newUsername, tempPassword, newRole, branchId);
                                System.out.println("Successfully hired " + newUsername + " as a " + newRole + "!");
                            } catch (NumberFormatException e) {
                                System.out.println("Invalid input. Branch ID must be a number.");
                            } catch (BankException e) {
                                System.out.println("Failed to create employee: " + e.getMessage());
                            } catch (DataAccessException e) {
                                System.out.println("Database error: " + e.getMessage());
                            }
                        }
                    } else {
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

    private static Customer registerCustomer(Scanner scanner, CustomerService customerService, SessionContext session) {
        System.out.println("\n--- Register New Customer ---");
        try {
            System.out.print("Enter First Name: ");
            String firstName = scanner.nextLine().trim();

            System.out.print("Enter Last Name: ");
            String lastName = scanner.nextLine().trim();

            System.out.print("Enter Email: ");
            String email = scanner.nextLine().trim();

            System.out.print("Enter Phone Number: ");
            String phone = scanner.nextLine().trim();

            System.out.print("Enter Address: ");
            String address = scanner.nextLine().trim();

            Customer customer = customerService.createCustomer(session, firstName, lastName, email, phone, address);
            System.out.println("\nCustomer registered successfully!");
            System.out.println("Customer ID: " + customer.getId());
            System.out.println("Name:        " + customer.getFullName());
            System.out.println("Email:       " + customer.getEmail());
            System.out.println("Phone:       " + customer.getPhoneNumber());
            return customer;
        } catch (BankException e) {
            System.out.println("Customer registration failed: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.out.println("Error creating customer: " + e.getMessage());
            return null;
        }
    }
}