package com.example.domain;

import com.example.exception.AccountClosedException;
import com.example.exception.AccountFrozenException;
import com.example.exception.InsufficientFundsException;
import com.example.exception.InvalidTransferException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class TransferPolicyTest {

    @Test
    public void testSuccessfulTransferCalculationWithoutMocks() {
        Account from = new Account(1, 100, "ACC-1", 1, AccountType.SAVINGS, new BigDecimal("500.00"), AccountStatus.ACTIVE, 0);
        Account to = new Account(2, 200, "ACC-2", 1, AccountType.SAVINGS, new BigDecimal("100.00"), AccountStatus.ACTIVE, 0);

        TransferPlan plan = TransferPolicy.calculateTransfer(from, to, new BigDecimal("150.00"));

        assertEquals(new BigDecimal("350.00"), plan.newFromBalance());
        assertEquals(new BigDecimal("250.00"), plan.newToBalance());
        assertEquals(new BigDecimal("150.00"), plan.transferAmount());
    }

    @Test
    public void testTransferFailsOnSelfTransfer() {
        Account account = new Account(1, 100, "ACC-1", 1, AccountType.SAVINGS, new BigDecimal("500.00"), AccountStatus.ACTIVE, 0);
        assertThrows(InvalidTransferException.class, () ->
            TransferPolicy.calculateTransfer(account, account, new BigDecimal("50.00"))
        );
    }

    @Test
    public void testTransferFailsWhenFromAccountFrozen() {
        Account from = new Account(1, 100, "ACC-1", 1, AccountType.SAVINGS, new BigDecimal("500.00"), AccountStatus.FROZEN, 0);
        Account to = new Account(2, 200, "ACC-2", 1, AccountType.SAVINGS, new BigDecimal("100.00"), AccountStatus.ACTIVE, 0);
        assertThrows(AccountFrozenException.class, () ->
            TransferPolicy.calculateTransfer(from, to, new BigDecimal("50.00"))
        );
    }

    @Test
    public void testTransferFailsOnInsufficientFunds() {
        Account from = new Account(1, 100, "ACC-1", 1, AccountType.SAVINGS, new BigDecimal("50.00"), AccountStatus.ACTIVE, 0);
        Account to = new Account(2, 200, "ACC-2", 1, AccountType.SAVINGS, new BigDecimal("100.00"), AccountStatus.ACTIVE, 0);
        assertThrows(InsufficientFundsException.class, () ->
            TransferPolicy.calculateTransfer(from, to, new BigDecimal("100.00"))
        );
    }
}
