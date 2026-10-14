package com.example.dao;

import com.example.domain.Transaction;
import java.sql.Connection;
import java.util.List;

public interface TransactionDAO {
    List<Transaction> getTransactionsForAccount(Integer accountId);
    List<Transaction> getTransactionsForAccount(Connection conn, Integer accountId);
    void insert(Transaction transaction);
    void insert(Connection conn, Transaction transaction);
}