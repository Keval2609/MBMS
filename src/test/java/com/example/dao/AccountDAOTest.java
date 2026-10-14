package com.example.dao;

import com.example.domain.Account;
import com.example.domain.AccountStatus;
import com.example.domain.AccountType;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class AccountDAOTest {

    @SuppressWarnings("unchecked")
    private <T> T createProxy(Class<T> iface, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, handler);
    }

    private DataSource createMockDataSource(Connection connection) {
        return createProxy(DataSource.class, (proxy, method, args) -> {
            if ("getConnection".equals(method.getName())) {
                return connection;
            }
            return null;
        });
    }

    private PreparedStatement createPreparedStatementWithResultSet(ResultSet rs, Map<Integer, Object> capturedParams) {
        return createProxy(PreparedStatement.class, (proxy, method, args) -> {
            String name = method.getName();
            if ("executeQuery".equals(name)) {
                return rs;
            }
            if ("setString".equals(name) || "setInt".equals(name) || "setBigDecimal".equals(name)) {
                if (capturedParams != null && args.length >= 2) {
                    capturedParams.put((Integer) args[0], args[1]);
                }
                return null;
            }
            if ("close".equals(name)) {
                return null;
            }
            return null;
        });
    }

    private PreparedStatement createPreparedStatementForUpdate(int rowsAffected, Map<Integer, Object> capturedParams) {
        return createProxy(PreparedStatement.class, (proxy, method, args) -> {
            String name = method.getName();
            if ("executeUpdate".equals(name)) {
                return rowsAffected;
            }
            if ("setString".equals(name) || "setInt".equals(name) || "setBigDecimal".equals(name)) {
                if (capturedParams != null && args.length >= 2) {
                    capturedParams.put((Integer) args[0], args[1]);
                }
                return null;
            }
            if ("close".equals(name)) {
                return null;
            }
            return null;
        });
    }

    private ResultSet createMockResultSet(Map<String, Object> values, boolean hasRow) {
        final boolean[] advanced = {false};
        return createProxy(ResultSet.class, (proxy, method, args) -> {
            String name = method.getName();
            if ("next".equals(name)) {
                if (!advanced[0] && hasRow) {
                    advanced[0] = true;
                    return true;
                }
                return false;
            }
            if ("getInt".equals(name)) {
                Object v = values.get(args[0]);
                return v != null ? ((Number) v).intValue() : 0;
            }
            if ("getString".equals(name)) {
                Object v = values.get(args[0]);
                return v != null ? v.toString() : null;
            }
            if ("getBigDecimal".equals(name)) {
                return (BigDecimal) values.get(args[0]);
            }
            if ("close".equals(name)) {
                return null;
            }
            return null;
        });
    }

    @Test
    public void testFindByAccountNumberWithFrozenStatusReturnsAccount() {
        Map<String, Object> row = new HashMap<>();
        row.put("id", 101);
        row.put("customer_id", 5);
        row.put("branch_id", 1);
        row.put("account_number", "ACC-101-FRZ");
        row.put("balance", new BigDecimal("1500.00"));
        row.put("status", "FROZEN");
        row.put("account_type", "SAVINGS");
        row.put("version", 2);

        ResultSet rs = createMockResultSet(row, true);
        Map<Integer, Object> params = new HashMap<>();
        PreparedStatement stmt = createPreparedStatementWithResultSet(rs, params);

        Connection conn = createProxy(Connection.class, (proxy, method, args) -> {
            if ("prepareStatement".equals(method.getName())) return stmt;
            if ("close".equals(method.getName())) return null;
            return null;
        });

        AccountDAOImpl dao = new AccountDAOImpl(createMockDataSource(conn));
        Optional<Account> result = dao.findByAccountNumber("ACC-101-FRZ");

        assertTrue(result.isPresent(), "Account with status FROZEN must be successfully returned");
        Account account = result.get();
        assertEquals(101, account.getId());
        assertEquals("ACC-101-FRZ", account.getAccountNumber());
        assertEquals(AccountStatus.FROZEN, account.getStatus(), "Status must be AccountStatus.FROZEN");
        assertEquals(AccountType.SAVINGS, account.getAccountType());
        assertEquals(new BigDecimal("1500.00"), account.getBalance());
        assertEquals(2, account.getVersion());
        assertEquals("ACC-101-FRZ", params.get(1));
    }

    @Test
    public void testFindByIdForUpdateUsesPessimisticLockSql() {
        Map<String, Object> row = new HashMap<>();
        row.put("id", 50);
        row.put("customer_id", 2);
        row.put("branch_id", 1);
        row.put("account_number", "ACC-50");
        row.put("balance", new BigDecimal("750.00"));
        row.put("status", "ACTIVE");
        row.put("account_type", "SAVINGS");
        row.put("version", 0);

        ResultSet rs = createMockResultSet(row, true);
        AtomicReference<String> executedSql = new AtomicReference<>();
        Map<Integer, Object> params = new HashMap<>();
        PreparedStatement stmt = createPreparedStatementWithResultSet(rs, params);

        Connection conn = createProxy(Connection.class, (proxy, method, args) -> {
            if ("prepareStatement".equals(method.getName())) {
                executedSql.set((String) args[0]);
                return stmt;
            }
            return null;
        });

        AccountDAOImpl dao = new AccountDAOImpl(createMockDataSource(conn));
        Optional<Account> result = dao.findByIdForUpdate(conn, 50);

        assertTrue(result.isPresent());
        assertTrue(executedSql.get().toUpperCase().contains("FOR UPDATE"), "SQL must contain FOR UPDATE");
        assertEquals(50, params.get(1));
    }

    @Test
    public void testUpdateBalanceIncrementsVersionAndEnforcesSingleRow() {
        Map<Integer, Object> params = new HashMap<>();
        PreparedStatement stmt = createPreparedStatementForUpdate(1, params);

        Connection conn = createProxy(Connection.class, (proxy, method, args) -> {
            if ("prepareStatement".equals(method.getName())) return stmt;
            return null;
        });

        AccountDAOImpl dao = new AccountDAOImpl(createMockDataSource(conn));
        dao.updateBalance(conn, 77, new BigDecimal("123.456"));

        assertEquals(new BigDecimal("123.46"), params.get(1), "Balance must be scaled to 2 decimal places with HALF_EVEN");
        assertEquals(77, params.get(2));
    }

    @Test
    public void testFindByAccountNumberNotFoundReturnsEmpty() {
        ResultSet rs = createMockResultSet(new HashMap<>(), false);
        PreparedStatement stmt = createPreparedStatementWithResultSet(rs, null);

        Connection conn = createProxy(Connection.class, (proxy, method, args) -> {
            if ("prepareStatement".equals(method.getName())) return stmt;
            if ("close".equals(method.getName())) return null;
            return null;
        });

        AccountDAOImpl dao = new AccountDAOImpl(createMockDataSource(conn));
        Optional<Account> result = dao.findByAccountNumber("NON-EXISTENT");

        assertTrue(result.isEmpty(), "Non-existent account must return Optional.empty()");
    }

    @Test
    public void testFindByAccountNumberSQLExceptionThrowsRuntimeException() {
        Connection conn = createProxy(Connection.class, (proxy, method, args) -> {
            if ("prepareStatement".equals(method.getName())) {
                throw new SQLException("Connection timed out");
            }
            return null;
        });

        AccountDAOImpl dao = new AccountDAOImpl(createMockDataSource(conn));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> dao.findByAccountNumber("ACC-ERR"));
        assertTrue(ex.getMessage().contains("Error querying account by account number"));
        assertInstanceOf(SQLException.class, ex.getCause());
    }

    @Test
    public void testFindByIdWithFrozenStatusReturnsAccount() {
        Map<String, Object> row = new HashMap<>();
        row.put("id", 202);
        row.put("customer_id", 6);
        row.put("branch_id", 2);
        row.put("account_number", "ACC-202");
        row.put("balance", new BigDecimal("5000.00"));
        row.put("status", "FROZEN");
        row.put("account_type", "CURRENT");
        row.put("version", 0);

        ResultSet rs = createMockResultSet(row, true);
        Map<Integer, Object> params = new HashMap<>();
        PreparedStatement stmt = createPreparedStatementWithResultSet(rs, params);

        Connection conn = createProxy(Connection.class, (proxy, method, args) -> {
            if ("prepareStatement".equals(method.getName())) return stmt;
            if ("close".equals(method.getName())) return null;
            return null;
        });

        AccountDAOImpl dao = new AccountDAOImpl(createMockDataSource(conn));
        Optional<Account> result = dao.findById(202);

        assertTrue(result.isPresent(), "Account with status FROZEN must be successfully returned by ID");
        Account account = result.get();
        assertEquals(202, account.getId());
        assertEquals(AccountStatus.FROZEN, account.getStatus());
        assertEquals(AccountType.CURRENT, account.getAccountType());
        assertEquals(202, params.get(1));
    }

    @Test
    public void testFindByIdNotFoundReturnsEmpty() {
        ResultSet rs = createMockResultSet(new HashMap<>(), false);
        PreparedStatement stmt = createPreparedStatementWithResultSet(rs, null);

        Connection conn = createProxy(Connection.class, (proxy, method, args) -> {
            if ("prepareStatement".equals(method.getName())) return stmt;
            if ("close".equals(method.getName())) return null;
            return null;
        });

        AccountDAOImpl dao = new AccountDAOImpl(createMockDataSource(conn));
        Optional<Account> result = dao.findById(999);

        assertTrue(result.isEmpty(), "Non-existent id must return Optional.empty()");
    }

    @Test
    public void testFindByIdSQLExceptionThrowsRuntimeException() {
        Connection conn = createProxy(Connection.class, (proxy, method, args) -> {
            if ("prepareStatement".equals(method.getName())) {
                throw new SQLException("Database unreachable");
            }
            return null;
        });

        AccountDAOImpl dao = new AccountDAOImpl(createMockDataSource(conn));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> dao.findById(999));
        assertTrue(ex.getMessage().contains("Error querying account by id: 999"));
        assertInstanceOf(SQLException.class, ex.getCause());
    }

    @Test
    public void testUpdateSuccessfulReturnsTrue() {
        Map<Integer, Object> params = new HashMap<>();
        PreparedStatement stmt = createPreparedStatementForUpdate(1, params);

        Connection conn = createProxy(Connection.class, (proxy, method, args) -> {
            if ("prepareStatement".equals(method.getName())) return stmt;
            if ("close".equals(method.getName())) return null;
            return null;
        });

        AccountDAOImpl dao = new AccountDAOImpl(createMockDataSource(conn));
        Account account = new Account(10, 1, "ACC-10", 1, AccountType.SAVINGS,
                new BigDecimal("200.00"), AccountStatus.FROZEN, 1);

        boolean success = dao.update(account);

        assertTrue(success, "Update must return true when 1 row is affected");
        assertEquals(new BigDecimal("200.00"), params.get(1));
        assertEquals("FROZEN", params.get(2));
        assertEquals(10, params.get(3));
        assertEquals(1, params.get(4));
    }

    @Test
    public void testUpdateOptimisticLockMismatchReturnsFalse() {
        PreparedStatement stmt = createPreparedStatementForUpdate(0, null);

        Connection conn = createProxy(Connection.class, (proxy, method, args) -> {
            if ("prepareStatement".equals(method.getName())) return stmt;
            if ("close".equals(method.getName())) return null;
            return null;
        });

        AccountDAOImpl dao = new AccountDAOImpl(createMockDataSource(conn));
        Account account = new Account(10, 1, "ACC-10", 1, AccountType.SAVINGS,
                new BigDecimal("200.00"), AccountStatus.ACTIVE, 1);

        boolean success = dao.update(account);

        assertFalse(success, "Update must return false when version mismatch updates 0 rows");
    }

    @Test
    public void testUpdateSQLExceptionThrowsRuntimeException() {
        Connection conn = createProxy(Connection.class, (proxy, method, args) -> {
            if ("prepareStatement".equals(method.getName())) {
                throw new SQLException("Deadlock encountered");
            }
            return null;
        });

        AccountDAOImpl dao = new AccountDAOImpl(createMockDataSource(conn));
        Account account = new Account(10, 1, "ACC-10", 1, AccountType.SAVINGS,
                new BigDecimal("200.00"), AccountStatus.ACTIVE, 1);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> dao.update(account));
        assertTrue(ex.getMessage().contains("Error updating account id: 10"));
        assertInstanceOf(SQLException.class, ex.getCause());
    }
}
