package com.example.dao;

import com.example.domain.Transaction;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAOImpl implements TransactionDAO {

    private final HikariDataSource dataSource;

    public TransactionDAOImpl(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<Transaction> getTransactionsForAccount(Integer accountId) {
        List<Transaction> transactions = new ArrayList<>();
        
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT * FROM transactions WHERE from_account_id = ? OR to_account_id = ? ORDER BY timestamp DESC")) {
            stmt.setInt(1, accountId);
            stmt.setInt(2, accountId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Transaction transaction = new Transaction();
                transaction.setId(rs.getInt("id"));
                transaction.setReferenceNumber(rs.getString("reference_number"));
                transaction.setFromAccountId(rs.getInt("from_account_id"));
                transaction.setToAccountId(rs.getInt("to_account_id"));
                transaction.setAmount(rs.getBigDecimal("amount"));
                transaction.setType(rs.getString("type"));
                transaction.setStatus(rs.getString("status"));
                transaction.setEmployeeId(rs.getInt("employee_id"));
                transaction.setCreatedAt(rs.getTimestamp("timestamp"));
                
                transactions.add(transaction);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return transactions;
    }
}