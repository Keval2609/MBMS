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

    private Account mapRowToAccount(ResultSet rs) throws SQLException {
        Account account = new Account();
        account.setId(rs.getInt("id"));
        account.setCustomerId(rs.getInt("customer_id"));
        account.setBranchId(rs.getInt("branch_id"));
        account.setAccountNumber(rs.getString("account_number"));
        account.setBalance(rs.getBigDecimal("balance"));
        account.setStatus(AccountStatus.valueOf(rs.getString("status")));
        account.setAccountType(AccountType.valueOf(rs.getString("account_type")));
        account.setVersion(rs.getInt("version"));
        return account;
    }
}