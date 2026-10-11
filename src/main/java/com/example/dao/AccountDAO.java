package com.example.dao;

import com.example.domain.Account;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Optional;

public interface AccountDAO {
    Optional<Account> findByAccountNumber(String accountNumber);
    Optional<Account> findByAccountNumber(Connection conn, String accountNumber);

    Optional<Account> findById(Integer id);
    Optional<Account> findById(Connection conn, Integer id);
    Optional<Account> findByIdForUpdate(Connection conn, Integer id);

    void updateBalance(Connection conn, Integer id, BigDecimal newBalance);
    boolean update(Account account);
    boolean update(Connection conn, Account account);
}