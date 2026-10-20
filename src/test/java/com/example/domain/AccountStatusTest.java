package com.example.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AccountStatusTest {

    @Test
    public void testEnumValuesMatchDatabaseSchema() {
        AccountStatus[] values = AccountStatus.values();
        assertEquals(3, values.length, "AccountStatus must have exactly 3 values to match DB schema");
        assertArrayEquals(new AccountStatus[]{AccountStatus.ACTIVE, AccountStatus.FROZEN, AccountStatus.CLOSED}, values);
    }

    @Test
    public void testFrozenValueOf() {
        AccountStatus status = AccountStatus.valueOf("FROZEN");
        assertEquals(AccountStatus.FROZEN, status);
        assertEquals("FROZEN", status.name());
    }

    @Test
    public void testActiveAndClosedValueOf() {
        assertEquals(AccountStatus.ACTIVE, AccountStatus.valueOf("ACTIVE"));
        assertEquals(AccountStatus.CLOSED, AccountStatus.valueOf("CLOSED"));
    }

    @Test
    public void testInactiveThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> AccountStatus.valueOf("INACTIVE"));
    }

    @Test
    public void testAssertCanTransactThrowsOnFrozenOrClosed() {
        Account active = new Account(1, 10, "ACC-1", 1, AccountType.SAVINGS, new java.math.BigDecimal("100.00"), AccountStatus.ACTIVE, 0);
        assertDoesNotThrow(active::assertCanTransact);

        Account frozen = new Account(2, 10, "ACC-2", 1, AccountType.SAVINGS, new java.math.BigDecimal("100.00"), AccountStatus.FROZEN, 0);
        com.example.exception.AccountFrozenException frozenEx = assertThrows(com.example.exception.AccountFrozenException.class, frozen::assertCanTransact);
        assertEquals(com.example.exception.ErrorCode.ACCOUNT_FROZEN, frozenEx.getErrorCode());

        Account closed = new Account(3, 10, "ACC-3", 1, AccountType.SAVINGS, new java.math.BigDecimal("100.00"), AccountStatus.CLOSED, 0);
        com.example.exception.AccountClosedException closedEx = assertThrows(com.example.exception.AccountClosedException.class, closed::assertCanTransact);
        assertEquals(com.example.exception.ErrorCode.ACCOUNT_CLOSED, closedEx.getErrorCode());
    }

    @Test
    public void testAssertSufficientFundsThrowsOnInsufficientBalance() {
        Account acc = new Account(1, 10, "ACC-1", 1, AccountType.SAVINGS, new java.math.BigDecimal("100.00"), AccountStatus.ACTIVE, 0);
        assertDoesNotThrow(() -> acc.assertSufficientFunds(new java.math.BigDecimal("50.00")));
        assertDoesNotThrow(() -> acc.assertSufficientFunds(new java.math.BigDecimal("100.00")));

        com.example.exception.InsufficientFundsException ex = assertThrows(
            com.example.exception.InsufficientFundsException.class,
            () -> acc.assertSufficientFunds(new java.math.BigDecimal("100.01"))
        );
        assertEquals(com.example.exception.ErrorCode.INSUFFICIENT_FUNDS, ex.getErrorCode());
    }
}

