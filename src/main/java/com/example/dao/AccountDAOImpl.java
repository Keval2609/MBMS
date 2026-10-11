package com.example.dao;

import com.example.domain.Account;
import com.example.domain.AccountStatus;
import com.example.domain.AccountType;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class AccountDAOImpl implements AccountDAO {

    private final DataSource dataSource;

    // Constructor injection
    public AccountDAOImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM accounts WHERE account_number = ?")) {
            stmt.setString(1, accountNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToAccount(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error querying account by account number: " + accountNumber, e);
        }
    }

    @Override
    public boolean update(Account account) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE accounts SET balance = ?, status = ?, version = version + 1 WHERE id = ? AND version = ?")) {
            stmt.setBigDecimal(1, account.getBalance());
            stmt.setString(2, account.getStatus().name());
            stmt.setInt(3, account.getId());
            stmt.setInt(4, account.getVersion());
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error updating account id: " + account.getId(), e);
        }
    }
    
    @Override
    public Optional<Account> findById(Integer id) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM accounts WHERE id = ?")) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToAccount(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error querying account by id: " + id, e);
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