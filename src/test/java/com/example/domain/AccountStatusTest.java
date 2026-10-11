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
}
