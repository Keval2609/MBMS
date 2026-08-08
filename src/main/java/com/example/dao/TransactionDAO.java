package com.example.dao;

import com.example.domain.Transaction;
import java.util.List;

public interface TransactionDAO {
    // We want to fetch all transactions where this account sent OR received money
    List<Transaction> getTransactionsForAccount(Integer accountId);
}