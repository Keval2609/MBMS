package com.example.service;

import com.example.dao.AccountDAO;
import com.example.dao.TransactionDAO;
import com.example.dao.TransactionManager;
import com.example.domain.*;
import com.example.exception.*;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class AccountServiceTest {

    private final SessionContext tellerSession = new SessionContext(1, 1, "teller", Role.TELLER);

    @SuppressWarnings("unchecked")
    private <T> T createProxy(Class<T> iface, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, handler);
    }

    private TransactionManager createPassThroughTransactionManager() {
        Connection conn = createProxy(Connection.class, (proxy, method, args) -> {
            if ("getAutoCommit".equals(method.getName())) return true;
            return null;
        });
        DataSource ds = createProxy(DataSource.class, (proxy, method, args) -> {
            if ("getConnection".equals(method.getName())) return conn;
            return null;
        });
        return new TransactionManager(ds);
    }

    @Test
    public void testTransferFailsOnSelfTransfer() {
        AccountService service = new AccountService(null, null, null);
        assertThrows(InvalidTransferException.class, () ->
            service.transferFunds(tellerSession, 1, 1, new BigDecimal("50.00"))
        );
    }

    @Test
    public void testTransferFailsOnZeroOrNegativeAmount() {
        AccountService service = new AccountService(null, null, null);
        assertThrows(IllegalArgumentException.class, () ->
            service.transferFunds(tellerSession, 1, 2, new BigDecimal("-10.00"))
        );
        assertThrows(IllegalArgumentException.class, () ->
            service.transferFunds(tellerSession, 1, 2, BigDecimal.ZERO)
        );
    }

    @Test
    public void testTransferFailsWhenSessionLacksRole() {
        SessionContext unauthorizedSession = new SessionContext(2, 1, "guest", (Role) null);
        AccountService service = new AccountService(null, null, null);

        assertThrows(UnauthorizedAccessException.class, () ->
            service.transferFunds(unauthorizedSession, 1, 2, new BigDecimal("50.00"))
        );
    }

    @Test
    public void testTransferFailsWhenSourceAccountIsFrozen() {
        TransactionManager tm = createPassThroughTransactionManager();

        Account frozen = new Account(1, 10, "ACC-1", 1, AccountType.SAVINGS, new BigDecimal("200.00"), AccountStatus.FROZEN, 0);
        Account active = new Account(2, 20, "ACC-2", 1, AccountType.SAVINGS, new BigDecimal("100.00"), AccountStatus.ACTIVE, 0);

        AccountDAO accountDAO = createProxy(AccountDAO.class, (proxy, method, args) -> {
            if ("findByIdForUpdate".equals(method.getName())) {
                Integer id = (Integer) args[1];
                if (id == 1) return Optional.of(frozen);
                if (id == 2) return Optional.of(active);
            }
            return Optional.empty();
        });

        TransactionDAO txnDAO = createProxy(TransactionDAO.class, (proxy, method, args) -> null);

        AccountService service = new AccountService(accountDAO, txnDAO, tm);
        assertThrows(AccountFrozenException.class, () ->
            service.transferFunds(tellerSession, 1, 2, new BigDecimal("50.00"))
        );
    }

    @Test
    public void testTransferFailsOnInsufficientFunds() {
        TransactionManager tm = createPassThroughTransactionManager();

        Account from = new Account(1, 10, "ACC-1", 1, AccountType.SAVINGS, new BigDecimal("20.00"), AccountStatus.ACTIVE, 0);
        Account to = new Account(2, 20, "ACC-2", 1, AccountType.SAVINGS, new BigDecimal("100.00"), AccountStatus.ACTIVE, 0);

        AccountDAO accountDAO = createProxy(AccountDAO.class, (proxy, method, args) -> {
            if ("findByIdForUpdate".equals(method.getName())) {
                Integer id = (Integer) args[1];
                if (id == 1) return Optional.of(from);
                if (id == 2) return Optional.of(to);
            }
            return Optional.empty();
        });

        TransactionDAO txnDAO = createProxy(TransactionDAO.class, (proxy, method, args) -> null);

        AccountService service = new AccountService(accountDAO, txnDAO, tm);
        assertThrows(InsufficientFundsException.class, () ->
            service.transferFunds(tellerSession, 1, 2, new BigDecimal("50.00"))
        );
    }

    @Test
    public void testTransferSuccessful() {
        TransactionManager tm = createPassThroughTransactionManager();

        Account from = new Account(1, 10, "ACC-1", 1, AccountType.SAVINGS, new BigDecimal("200.00"), AccountStatus.ACTIVE, 0);
        Account to = new Account(2, 20, "ACC-2", 1, AccountType.SAVINGS, new BigDecimal("100.00"), AccountStatus.ACTIVE, 0);

        AtomicReference<BigDecimal> updatedBalFrom = new AtomicReference<>();
        AtomicReference<BigDecimal> updatedBalTo = new AtomicReference<>();
        AtomicReference<Transaction> insertedTxn = new AtomicReference<>();

        AccountDAO accountDAO = createProxy(AccountDAO.class, (proxy, method, args) -> {
            String name = method.getName();
            if ("findByIdForUpdate".equals(name)) {
                Integer id = (Integer) args[1];
                if (id == 1) return Optional.of(from);
                if (id == 2) return Optional.of(to);
            }
            if ("updateBalance".equals(name)) {
                Integer id = (Integer) args[1];
                BigDecimal bal = (BigDecimal) args[2];
                if (id == 1) updatedBalFrom.set(bal);
                if (id == 2) updatedBalTo.set(bal);
                return null;
            }
            return null;
        });

        TransactionDAO txnDAO = createProxy(TransactionDAO.class, (proxy, method, args) -> {
            if ("insert".equals(method.getName())) {
                insertedTxn.set((Transaction) args[1]);
                return null;
            }
            return null;
        });

        AccountService service = new AccountService(accountDAO, txnDAO, tm);
        Transaction result = service.transferFunds(tellerSession, 1, 2, new BigDecimal("50.00"));

        assertNotNull(result);
        assertEquals(new BigDecimal("150.00"), updatedBalFrom.get());
        assertEquals(new BigDecimal("150.00"), updatedBalTo.get());
        assertNotNull(insertedTxn.get());
        assertEquals(TransactionType.TRANSFER, insertedTxn.get().getType());
        assertEquals(TransactionStatus.COMPLETED, insertedTxn.get().getStatus());
    }

    @Test
    public void testDepositSuccessful() {
        TransactionManager tm = createPassThroughTransactionManager();
        Account target = new Account(5, 10, "ACC-5", 1, AccountType.SAVINGS, new BigDecimal("300.00"), AccountStatus.ACTIVE, 0);
        AtomicReference<BigDecimal> updatedBal = new AtomicReference<>();
        AtomicReference<Transaction> insertedTxn = new AtomicReference<>();

        AccountDAO accountDAO = createProxy(AccountDAO.class, (proxy, method, args) -> {
            if ("findByIdForUpdate".equals(method.getName())) {
                return Optional.of(target);
            }
            if ("updateBalance".equals(method.getName())) {
                updatedBal.set((BigDecimal) args[2]);
                return null;
            }
            return null;
        });

        TransactionDAO txnDAO = createProxy(TransactionDAO.class, (proxy, method, args) -> {
            if ("insert".equals(method.getName())) {
                insertedTxn.set((Transaction) args[1]);
                return null;
            }
            return null;
        });

        AccountService service = new AccountService(accountDAO, txnDAO, tm);
        Transaction txn = service.deposit(tellerSession, 5, new BigDecimal("100.50"));

        assertEquals(new BigDecimal("400.50"), updatedBal.get());
        assertNull(insertedTxn.get().getFromAccountId());
        assertEquals(5, insertedTxn.get().getToAccountId());
        assertEquals(TransactionType.DEPOSIT, insertedTxn.get().getType());
    }

    @Test
    public void testWithdrawSuccessfulAndFailsOnInsufficientFunds() {
        TransactionManager tm = createPassThroughTransactionManager();
        Account target = new Account(5, 10, "ACC-5", 1, AccountType.SAVINGS, new BigDecimal("100.00"), AccountStatus.ACTIVE, 0);
        AtomicReference<BigDecimal> updatedBal = new AtomicReference<>();

        AccountDAO accountDAO = createProxy(AccountDAO.class, (proxy, method, args) -> {
            if ("findByIdForUpdate".equals(method.getName())) {
                return Optional.of(target);
            }
            if ("updateBalance".equals(method.getName())) {
                updatedBal.set((BigDecimal) args[2]);
                return null;
            }
            return null;
        });

        TransactionDAO txnDAO = createProxy(TransactionDAO.class, (proxy, method, args) -> null);
        AccountService service = new AccountService(accountDAO, txnDAO, tm);

        Transaction txn = service.withdraw(tellerSession, 5, new BigDecimal("40.00"));
        assertEquals(new BigDecimal("60.00"), updatedBal.get());
        assertEquals(TransactionType.WITHDRAWAL, txn.getType());

        // Now attempt over-withdrawal
        assertThrows(InsufficientFundsException.class, () ->
            service.withdraw(tellerSession, 5, new BigDecimal("150.00"))
        );
    }
}
