package com.example.factory;

import com.example.domain.Account;
import com.example.domain.AccountStatus;
import com.example.domain.AccountType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AccountFactoryTest {

    @Test
    @DisplayName("Successfully creates a SAVINGS account with rounded initial balance")
    void shouldCreateSavingsAccountSuccessfully() {
        Account account = AccountFactory.create(
                AccountType.SAVINGS,
                10,
                1,
                "ACC10001",
                new BigDecimal("500.505")
        );

        assertNotNull(account);
        assertNull(account.getId());
        assertEquals(10, account.getCustomerId());
        assertEquals(1, account.getBranchId());
        assertEquals("ACC10001", account.getAccountNumber());
        assertEquals(AccountType.SAVINGS, account.getAccountType());
        assertEquals(new BigDecimal("500.50"), account.getBalance());
        assertEquals(AccountStatus.ACTIVE, account.getStatus());
        assertEquals(0, account.getVersion());
    }

    @Test
    @DisplayName("Defaults null account type to SAVINGS and null balance to 0.00")
    void shouldDefaultAccountTypeAndBalance() {
        Account account = AccountFactory.create(
                null,
                15,
                2,
                "ACC10002",
                null
        );

        assertEquals(AccountType.SAVINGS, account.getAccountType());
        assertEquals(new BigDecimal("0.00"), account.getBalance());
    }

    @Test
    @DisplayName("Rejects negative initial deposit")
    void shouldRejectNegativeInitialDeposit() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                AccountFactory.create(
                        AccountType.CURRENT,
                        10,
                        1,
                        "ACC10003",
                        new BigDecimal("-50.00")
                )
        );
        assertTrue(ex.getMessage().contains("negative"));
    }

    @Test
    @DisplayName("Rejects missing customer ID or branch ID or account number")
    void shouldRejectMissingRequiredFields() {
        assertThrows(IllegalArgumentException.class, () ->
                AccountFactory.create(AccountType.SAVINGS, null, 1, "ACC10001", BigDecimal.ZERO));

        assertThrows(IllegalArgumentException.class, () ->
                AccountFactory.create(AccountType.SAVINGS, 10, null, "ACC10001", BigDecimal.ZERO));

        assertThrows(IllegalArgumentException.class, () ->
                AccountFactory.create(AccountType.SAVINGS, 10, 1, "   ", BigDecimal.ZERO));
    }
}
