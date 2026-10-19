package com.example.dao;

import com.example.domain.Employee;
import com.example.domain.Role;
import com.example.exception.DataAccessException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class EmployeeDAOImpl implements EmployeeDAO {

    private final DataSource dataSource;

    public EmployeeDAOImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<Employee> findByUsername(String username) {
        try (Connection conn = dataSource.getConnection()) {
            return findByUsername(conn, username);
        } catch (SQLException e) {
            throw new DataAccessException("Error querying employee by username: " + username, e);
        }
    }

    @Override
    public Optional<Employee> findByUsername(Connection conn, String username) {
        String sql = "SELECT * FROM employees WHERE username = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Employee employee = new Employee();
                    employee.setId(rs.getInt("id"));
                    employee.setUsername(rs.getString("username"));
                    employee.setPasswordHash(rs.getString("password_hash"));
                    employee.setRole(Role.fromString(rs.getString("role")));
                    employee.setBranchId(rs.getObject("branch_id", Integer.class));
                    return Optional.of(employee);
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error querying employee by username: " + username, e);
        }
    }

    @Override
    public void insert(Employee employee) {
        try (Connection conn = dataSource.getConnection()) {
            insert(conn, employee);
        } catch (SQLException e) {
            throw new DataAccessException("Error inserting employee: " + employee.getUsername(), e);
        }
    }

    @Override
    public void insert(Connection conn, Employee employee) {
        String sql = "INSERT INTO employees (branch_id, username, password_hash, role) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, employee.getBranchId(), java.sql.Types.INTEGER);
            stmt.setString(2, employee.getUsername());
            stmt.setString(3, employee.getPasswordHash());
            stmt.setString(4, employee.getRole().name());
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected != 1) {
                throw new DataAccessException("Expected exactly 1 row inserted for employee, but got " + rowsAffected);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error inserting employee: " + employee.getUsername(), e);
        }
    }
}