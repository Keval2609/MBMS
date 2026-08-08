package com.example.dao;

import com.example.domain.Account;
import com.example.domain.AccountStatus;
import com.example.domain.AccountType;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

public class AccountDAOImpl implements AccountDAO {

    private final HikariDataSource dataSource;

    // Constructor injection
    public AccountDAOImpl(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM accounts WHERE account_number = ?")) {
            stmt.setString(1, accountNumber);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Account account = new Account();
                account.setId(rs.getInt("id"));
                account.setCustomerId(rs.getInt("customer_id"));
                account.setBranchId(rs.getInt("branch_id"));
                account.setAccountNumber(rs.getString("account_number"));
                account.setBalance(rs.getBigDecimal("balance"));
                account.setStatus(AccountStatus.valueOf(rs.getString("status")));
                account.setAccountType(AccountType.valueOf(rs.getString("account_type")));
                account.setVersion(rs.getInt("version"));
                return Optional.of(account);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty(); 
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
            return rowsAffected > 0; // Returns true if the update was successful
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}