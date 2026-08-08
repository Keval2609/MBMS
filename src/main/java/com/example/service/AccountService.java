package com.example.service;

import com.zaxxer.hikari.HikariDataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class AccountService {

    private final HikariDataSource dataSource;

    public AccountService(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean transferFunds(Integer fromAccountId, Integer toAccountId, BigDecimal amount, Integer employeeId) {
        // 1. Basic validation (amount must be > 0, accounts can't be the same)
        if (amount.compareTo(BigDecimal.ZERO) <= 0 || fromAccountId.equals(toAccountId)) {
            return false; 
        }

        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            // Step 1: Deadlock Prevention. 
            Integer lowerId = Math.min(fromAccountId, toAccountId);
            Integer higherId = Math.max(fromAccountId, toAccountId);
             
            // Step 2: The Locks. 
            String lockSql = ("SELECT balance FROM accounts WHERE id = ? FOR UPDATE");
            
            BigDecimal fromAccountBalance = null;
            BigDecimal toAccountBalance = null;

            try (PreparedStatement lockStmt = conn.prepareStatement(lockSql)) {
                // Lock the lower ID
                lockStmt.setInt(1, lowerId);
                try ( ResultSet rsLower = lockStmt.executeQuery()) {
                    if (!rsLower.next()) throw new RuntimeException("Account not found: " + lowerId);
                    if (lowerId.equals(fromAccountId)) {
                        fromAccountBalance = rsLower.getBigDecimal("balance");
                    } else {
                        toAccountBalance = rsLower.getBigDecimal("balance");
                    }
                }

                // Lock the higher ID
                lockStmt.setInt(1, higherId);
                try (ResultSet rsHigher = lockStmt.executeQuery()) {
                    if (!rsHigher.next()) throw new RuntimeException("Account not found: " + higherId); 
                    if (higherId.equals(fromAccountId)) {
                        fromAccountBalance = rsHigher.getBigDecimal("balance");
                    } else {
                        toAccountBalance = rsHigher.getBigDecimal("balance");
                    }
                    
                }
            }
            // Step 3: The Math.
            if (fromAccountBalance.compareTo(amount) < 0) {
                throw new RuntimeException("Insufficient funds in account: " + fromAccountId);
            } else {
                fromAccountBalance = fromAccountBalance.subtract(amount);
                toAccountBalance = toAccountBalance.add(amount);
            }

            // Step 4: The Updates.
            PreparedStatement updateStmt = conn.prepareStatement("UPDATE accounts SET balance = ? WHERE id = ?");
            updateStmt.setBigDecimal(1, fromAccountBalance);
            updateStmt.setInt(2, fromAccountId);
            updateStmt.executeUpdate();
            updateStmt.setBigDecimal(1, toAccountBalance);
            updateStmt.setInt(2, toAccountId);
            updateStmt.executeUpdate();

            // Step 5: The Audit Log.
            String insertTransactionSql = "INSERT INTO transactions (reference_number, from_account_id, to_account_id, amount, type, status, employee_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertTransactionSql)) {
                insertStmt.setString(1, UUID.randomUUID().toString());
                insertStmt.setInt(2, fromAccountId);
                insertStmt.setInt(3, toAccountId);
                insertStmt.setBigDecimal(4, amount);
                insertStmt.setString(5, "TRANSFER");
                insertStmt.setString(6, "COMPLETED");
                insertStmt.setInt(7, employeeId);
                insertStmt.executeUpdate();
            }
            conn.commit();
            return true;

        } catch (Exception e) {
            System.err.println("Transaction failed! Rolling back changes.");
            e.printStackTrace();
            if (conn != null) {
                try {
                    // THE SHIELD: Undo everything if ANY step failed
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    // Always put the connection back the way you found it
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}