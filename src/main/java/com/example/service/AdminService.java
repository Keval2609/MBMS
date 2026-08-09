package com.example.service;

import com.zaxxer.hikari.HikariDataSource;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class AdminService {

    private final HikariDataSource dataSource;

    public AdminService(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean createEmployee(String username, String rawPassword, String role, Integer branchId) {
        
        // 1. Hash the password before it ever touches the database!
        String hashedPassword = BCrypt.hashpw(rawPassword, BCrypt.gensalt());

        try (Connection conn = dataSource.getConnection()) {
            String sql = "INSERT INTO employees (branch_id, username, password_hash, role) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, branchId);
                stmt.setString(2, username);
                stmt.setString(3, hashedPassword);
                stmt.setString(4, role);
                int rowsAffected = stmt.executeUpdate();
                return rowsAffected > 0; // Return true if at least one row was inserted
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false; 
    }
}