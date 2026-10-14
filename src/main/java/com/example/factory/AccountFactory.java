package com.example.factory;

import com.example.domain.Account;
import com.example.domain.AccountStatus;
import com.example.domain.AccountType;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class AccountFactory {

    private AccountFactory() {
        // Prevent instantiation of utility factory class
    }

    public static Account create(AccountType type, Integer customerId, Integer branchId, 
                                 String accountNumber, BigDecimal initialBalance) {
        if (type == null) {
            type = AccountType.SAVINGS;
        }
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID is required to open an account");
        }
        if (branchId == null) {
            throw new IllegalArgumentException("Branch ID is required to open an account");
        }
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Account number is required");
        }

        BigDecimal balance = initialBalance != null 
                ? initialBalance.setScale(2, RoundingMode.HALF_EVEN) 
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);

        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Initial deposit cannot be negative");
        }

        return new Account(
                null,
                customerId,
                accountNumber.trim(),
                branchId,
                type,
                balance,
                AccountStatus.ACTIVE,
                0
        );
    }
}
