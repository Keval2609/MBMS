package com.example.dao;

import com.example.domain.Account;
import java.util.Optional;

public interface AccountDAO {
    Optional<Account> findById(Integer id);
    Optional<Account> findByAccountNumber(String accountNumber);
    boolean update(Account account); // Returns true if successful (for optimistic locking)
}