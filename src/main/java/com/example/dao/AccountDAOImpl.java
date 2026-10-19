package com.example.dao;

import com.example.domain.Account;
import com.example.domain.AccountStatus;
import com.example.domain.AccountType;
import com.example.exception.DataAccessException;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class AccountDAOImpl implements AccountDAO {

    private final DataSource dataSource;

    public AccountDAOImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        try (Connection conn = dataSource.getConnection()) {
            return findByAccountNumber(conn, accountNumber);
        } catch (SQLException e) {
            throw new DataAccessException("Error querying account by account number: " + accountNumber, e);
        }
    }

    @Override
    public Optional<Account> findByAccountNumber(Connection conn, String accountNumber) {
        String sql = "SELECT * FROM accounts WHERE account_number = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, accountNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToAccount(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error querying account by account number: " + accountNumber, e);
        }
    }

    @Override
    public Optional<Account> findById(Integer id) {
        try (Connection conn = dataSource.getConnection()) {
            return findById(conn, id);
        } catch (SQLException e) {
            throw new DataAccessException("Error querying account by id: " + id, e);
        }
    }

    @Override
    public Optional<Account> findById(Connection conn, Integer id) {
        String sql = "SELECT * FROM accounts WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToAccount(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error querying account by id: " + id, e);
        }
    }

    @Override
    public Optional<Account> findByIdForUpdate(Connection conn, Integer id) {
        // Explicit pessimistic locking to serialize mutations and prevent deadlocks
        String sql = "SELECT * FROM accounts WHERE id = ? FOR UPDATE";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToAccount(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error acquiring pessimistic row lock for account id: " + id, e);
        }
    }

    @Override
    public void updateBalance(Connection conn, Integer id, BigDecimal newBalance) {
        String sql = "UPDATE accounts SET balance = ?, version = version + 1 WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, newBalance.setScale(2, RoundingMode.HALF_EVEN));
            stmt.setInt(2, id);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected != 1) {
                throw new DataAccessException("Expected exactly 1 row updated for account " + id + ", but was " + rowsAffected);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error updating balance for account id: " + id, e);
        }
    }

    @Override
    public boolean update(Account account) {
        try (Connection conn = dataSource.getConnection()) {
            return update(conn, account);
        } catch (SQLException e) {
            throw new DataAccessException("Error updating account id: " + account.getId(), e);
        }
    }

    @Override
    public boolean update(Connection conn, Account account) {
        String sql = "UPDATE accounts SET balance = ?, status = ?, version = version + 1 WHERE id = ? AND version = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, account.getBalance().setScale(2, RoundingMode.HALF_EVEN));
            stmt.setString(2, account.getStatus().name());
            stmt.setInt(3, account.getId());
            stmt.setInt(4, account.getVersion());
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Error updating account id: " + account.getId(), e);
        }
    }

    @Override
    public Account insert(Account account) {
        try (Connection conn = dataSource.getConnection()) {
            return insert(conn, account);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert account: " + account.getAccountNumber(), e);
        }
    }

    @Override
    public Account insert(Connection conn, Account account) {
        String sql = "INSERT INTO accounts (customer_id, account_number, branch_id, account_type, balance, status, version) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setObject(1, account.getCustomerId());
            stmt.setString(2, account.getAccountNumber());
            stmt.setObject(3, account.getBranchId());
            stmt.setString(4, account.getAccountType().name());
            stmt.setBigDecimal(5, account.getBalance() != null 
                    ? account.getBalance().setScale(2, RoundingMode.HALF_EVEN) 
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN));
            stmt.setString(6, account.getStatus() != null ? account.getStatus().name() : AccountStatus.ACTIVE.name());
            stmt.setInt(7, account.getVersion() != null ? account.getVersion() : 0);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DataAccessException("Creating account failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    account.setId(generatedKeys.getInt(1));
                } else {
                    throw new DataAccessException("Creating account failed, no ID obtained.");
                }
            }
            return account;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert account: " + account.getAccountNumber(), e);
        }
    }

    @Override
    public String generateNextAccountNumber(Connection conn) {
        String sql = "SELECT COALESCE(MAX(id), 0) FROM accounts";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            int maxId = rs.next() ? rs.getInt(1) : 0;
            int nextNumber = 10000 + maxId + 1;
            return String.format("ACC%05d", nextNumber);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to generate next account number", e);
        }
    }

    private Account mapRowToAccount(ResultSet rs) throws SQLException {
        Account account = new Account();
        account.setId(rs.getInt("id"));
        account.setCustomerId(rs.getObject("customer_id", Integer.class));
        account.setBranchId(rs.getObject("branch_id", Integer.class));
        account.setAccountNumber(rs.getString("account_number"));
        account.setBalance(rs.getBigDecimal("balance"));
        account.setStatus(AccountStatus.valueOf(rs.getString("status")));
        account.setAccountType(AccountType.valueOf(rs.getString("account_type")));
        account.setVersion(rs.getObject("version", Integer.class));
        return account;
    }
}