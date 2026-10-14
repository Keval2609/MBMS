package com.example.dao;

import com.example.domain.Customer;
import com.example.exception.DataAccessException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CustomerDAOImpl implements CustomerDAO {

    private final DataSource dataSource;

    public CustomerDAOImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<Customer> findById(Integer id) {
        try (Connection conn = dataSource.getConnection()) {
            return findById(conn, id);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query customer by ID: " + id, e);
        }
    }

    @Override
    public Optional<Customer> findById(Connection conn, Integer id) {
        String sql = "SELECT id, first_name, last_name, email, phone_number, address, created_at FROM customers WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToCustomer(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query customer by ID: " + id, e);
        }
    }

    @Override
    public Customer insert(Customer customer) {
        try (Connection conn = dataSource.getConnection()) {
            return insert(conn, customer);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert customer", e);
        }
    }

    @Override
    public Customer insert(Connection conn, Customer customer) {
        String sql = "INSERT INTO customers (first_name, last_name, email, phone_number, address) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, customer.getFirstName());
            stmt.setString(2, customer.getLastName());
            stmt.setString(3, customer.getEmail());
            stmt.setString(4, customer.getPhoneNumber());
            stmt.setString(5, customer.getAddress());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DataAccessException("Creating customer failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    customer.setId(generatedKeys.getInt(1));
                } else {
                    throw new DataAccessException("Creating customer failed, no ID obtained.");
                }
            }
            return customer;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert customer: " + customer.getEmail(), e);
        }
    }

    @Override
    public List<Customer> findAll() {
        try (Connection conn = dataSource.getConnection()) {
            return findAll(conn);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve customers", e);
        }
    }

    @Override
    public List<Customer> findAll(Connection conn) {
        String sql = "SELECT id, first_name, last_name, email, phone_number, address, created_at FROM customers ORDER BY id ASC";
        List<Customer> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToCustomer(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve customers", e);
        }
    }

    private Customer mapRowToCustomer(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setId(rs.getInt("id"));
        c.setFirstName(rs.getString("first_name"));
        c.setLastName(rs.getString("last_name"));
        c.setEmail(rs.getString("email"));
        c.setPhoneNumber(rs.getString("phone_number"));
        c.setAddress(rs.getString("address"));
        c.setCreatedAt(rs.getTimestamp("created_at"));
        return c;
    }
}
