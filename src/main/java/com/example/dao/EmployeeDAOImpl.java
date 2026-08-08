package com.example.dao;

import com.example.domain.Employee;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

public class EmployeeDAOImpl implements EmployeeDAO {

    private final HikariDataSource dataSource;

    public EmployeeDAOImpl(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<Employee> findByUsername(String username) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM employees WHERE username = ?")) {
            stmt.setString(1, username);    
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Employee employee = new Employee();
                employee.setId(rs.getInt("id"));
                employee.setUsername(rs.getString("username"));
                employee.setPasswordHash(rs.getString("password_hash"));
                employee.setRole(rs.getString("role"));
                employee.setBranchId(rs.getInt("branch_id"));
                return Optional.of(employee);
            }
        } catch (Exception e) {
            e.printStackTrace();    
        } return Optional.empty();
    }
}