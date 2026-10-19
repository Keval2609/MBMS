package com.example.dao;

import com.example.domain.Transaction;
import com.example.domain.TransactionStatus;
import com.example.domain.TransactionType;
import com.example.exception.DataAccessException;

import javax.sql.DataSource;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAOImpl implements TransactionDAO {

    private final DataSource dataSource;

    public TransactionDAOImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<Transaction> getTransactionsForAccount(Integer accountId) {
        try (Connection conn = dataSource.getConnection()) {
            return getTransactionsForAccount(conn, accountId);
        } catch (SQLException e) {
            throw new DataAccessException("Error fetching transactions for account: " + accountId, e);
        }
    }

    @Override
    public List<Transaction> getTransactionsForAccount(Connection conn, Integer accountId) {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE from_account_id = ? OR to_account_id = ? ORDER BY timestamp DESC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, accountId);
            stmt.setInt(2, accountId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Transaction transaction = new Transaction();
                    transaction.setId(rs.getInt("id"));
                    transaction.setReferenceNumber(rs.getString("reference_number"));
                    // Use getObject to properly map nullable integer columns instead of defaulting to 0
                    transaction.setFromAccountId(rs.getObject("from_account_id", Integer.class));
                    transaction.setToAccountId(rs.getObject("to_account_id", Integer.class));
                    transaction.setAmount(rs.getBigDecimal("amount"));
                    transaction.setType(TransactionType.valueOf(rs.getString("type")));
                    transaction.setStatus(TransactionStatus.valueOf(rs.getString("status")));
                    transaction.setEmployeeId(rs.getInt("employee_id"));
                    java.sql.Timestamp ts = rs.getTimestamp("timestamp");
                    transaction.setCreatedAt(ts != null ? ts.toInstant() : null);
                    transactions.add(transaction);
                }
            }
            return transactions;
        } catch (SQLException e) {
            throw new DataAccessException("Error fetching transactions for account: " + accountId, e);
        }
    }

    @Override
    public void insert(Transaction transaction) {
        try (Connection conn = dataSource.getConnection()) {
            insert(conn, transaction);
        } catch (SQLException e) {
            throw new DataAccessException("Error inserting transaction", e);
        }
    }

    @Override
    public void insert(Connection conn, Transaction transaction) {
        String sql = "INSERT INTO transactions (reference_number, from_account_id, to_account_id, amount, type, status, employee_id) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, transaction.getReferenceNumber());
            stmt.setObject(2, transaction.getFromAccountId(), java.sql.Types.INTEGER);
            stmt.setObject(3, transaction.getToAccountId(), java.sql.Types.INTEGER);
            stmt.setBigDecimal(4, transaction.getAmount().setScale(2, RoundingMode.HALF_EVEN));
            stmt.setString(5, transaction.getType().name());
            stmt.setString(6, transaction.getStatus().name());
            stmt.setInt(7, transaction.getEmployeeId());
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected != 1) {
                throw new DataAccessException("Expected exactly 1 row inserted for transaction, but got " + rowsAffected);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error inserting transaction with reference: " + transaction.getReferenceNumber(), e);
        }
    }
}