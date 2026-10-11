package com.example.dao;

import com.example.exception.DataAccessException;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionManagerTest {

    @SuppressWarnings("unchecked")
    private <T> T createProxy(Class<T> iface, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, handler);
    }

    @Test
    public void testInTransactionCommitsOnSuccess() {
        AtomicBoolean autoCommitSetFalse = new AtomicBoolean(false);
        AtomicBoolean committed = new AtomicBoolean(false);
        AtomicBoolean closed = new AtomicBoolean(false);

        Connection conn = createProxy(Connection.class, (proxy, method, args) -> {
            String name = method.getName();
            if ("setAutoCommit".equals(name)) {
                if (Boolean.FALSE.equals(args[0])) {
                    autoCommitSetFalse.set(true);
                }
                return null;
            }
            if ("getAutoCommit".equals(name)) return true;
            if ("commit".equals(name)) {
                committed.set(true);
                return null;
            }
            if ("close".equals(name)) {
                closed.set(true);
                return null;
            }
            return null;
        });

        DataSource ds = createProxy(DataSource.class, (proxy, method, args) -> {
            if ("getConnection".equals(method.getName())) return conn;
            return null;
        });

        TransactionManager tm = new TransactionManager(ds);
        String result = tm.inTransaction(connection -> {
            assertSame(conn, connection);
            return "SUCCESS";
        });

        assertEquals("SUCCESS", result);
        assertTrue(autoCommitSetFalse.get(), "AutoCommit must be set to false");
        assertTrue(committed.get(), "Transaction must be committed");
        assertTrue(closed.get(), "Connection must be closed");
    }

    @Test
    public void testInTransactionRollsBackOnException() {
        AtomicBoolean rolledBack = new AtomicBoolean(false);

        Connection conn = createProxy(Connection.class, (proxy, method, args) -> {
            String name = method.getName();
            if ("setAutoCommit".equals(name)) return null;
            if ("getAutoCommit".equals(name)) return true;
            if ("rollback".equals(name)) {
                rolledBack.set(true);
                return null;
            }
            if ("close".equals(name)) return null;
            return null;
        });

        DataSource ds = createProxy(DataSource.class, (proxy, method, args) -> {
            if ("getConnection".equals(method.getName())) return conn;
            return null;
        });

        TransactionManager tm = new TransactionManager(ds);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            tm.inTransaction(connection -> {
                throw new IllegalStateException("Simulated business error");
            });
        });

        assertEquals("Simulated business error", ex.getMessage());
        assertTrue(rolledBack.get(), "Connection must be rolled back on exception");
    }

    @Test
    public void testInTransactionWrapsSQLExceptionInDataAccessException() {
        Connection conn = createProxy(Connection.class, (proxy, method, args) -> {
            if ("setAutoCommit".equals(name(method))) return null;
            if ("getAutoCommit".equals(name(method))) return true;
            if ("rollback".equals(name(method))) return null;
            if ("close".equals(name(method))) return null;
            return null;
        });

        DataSource ds = createProxy(DataSource.class, (proxy, method, args) -> {
            if ("getConnection".equals(name(method))) return conn;
            return null;
        });

        TransactionManager tm = new TransactionManager(ds);

        DataAccessException ex = assertThrows(DataAccessException.class, () -> {
            tm.inTransaction(connection -> {
                throw new SQLException("Deadlock found");
            });
        });

        assertTrue(ex.getMessage().contains("Transaction rolled back"));
        assertInstanceOf(SQLException.class, ex.getCause());
    }

    private String name(java.lang.reflect.Method m) {
        return m.getName();
    }
}
