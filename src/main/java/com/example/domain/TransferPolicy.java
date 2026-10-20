package com.example.domain;

import com.example.exception.InvalidTransferException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class TransferPolicy {

    private TransferPolicy() {}

    public static TransferPlan calculateTransfer(Account fromAccount, Account toAccount, BigDecimal amount) {
        if (fromAccount.getId() != null && fromAccount.getId().equals(toAccount.getId())) {
            throw new InvalidTransferException("Cannot transfer funds to the same account (ID: " + fromAccount.getId() + ")");
        }

        fromAccount.assertCanTransact();
        toAccount.assertCanTransact();

        BigDecimal normalizedAmount = amount.setScale(2, RoundingMode.HALF_EVEN);
        fromAccount.assertSufficientFunds(normalizedAmount);

        BigDecimal newFromBalance = fromAccount.getBalance().subtract(normalizedAmount);
        BigDecimal newToBalance = toAccount.getBalance().add(normalizedAmount);

        return new TransferPlan(newFromBalance, newToBalance, normalizedAmount);
    }
}
