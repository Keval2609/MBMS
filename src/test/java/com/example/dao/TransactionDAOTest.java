package com.example.dao;

import com.example.domain.Transaction;
import com.example.domain.TransactionStatus;
import com.example.domain.TransactionType;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionDAOTest {

    @SuppressWarnings("unchecked")
    private <T> T createProxy(Class<T> iface, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, handler);
    }

    @Test
    public void testGetTransactionsMapsNullableColumnsToNull() {
        AtomicBoolean nextCalled = new AtomicBoolean(false);
        ResultSet rs = createProxy(ResultSet.class, (proxy, method, args) -> {
            String name = method.getName();
            if ("next".equals(name)) {
                return nextCalled.compareAndSet(false, true);
            }
            if ("getInt".equals(name)) {
                if ("id".equals(args[0])) return 1;
                if ("employee_id".equals(args[0])) return 99;
                return 0;
            }
            if ("getObject".equals(name)) {
                if ("from_account_id".equals(args[0])) return null; // Nullable for direct deposit
                if ("to_account_id".equals(args[0])) return 5;
                return null;
            }
            if ("getString".equals(name)) {
                if ("reference_number".equals(args[0])) return "REF-DEP-1";
                if ("type".equals(args[0])) return "DEPOSIT";
                if ("status".equals(args[0])) return "COMPLETED";
                return "";
            }
            if ("getBigDecimal".equals(name)) return new BigDecimal("100.00");
            if ("getTimestamp".equals(name)) return new Timestamp(System.currentTimeMillis());
            return null;
        });

        PreparedStatement stmt = createProxy(PreparedStatement.class, (proxy, method, args) -> {
            if ("executeQuery".equals(method.getName())) return rs;
            return null;
        });

        Connection conn = createProxy(Connection.class, (proxy, method, args) -> {
            if ("prepareStatement".equals(method.getName())) return stmt;
            return null;
        });

        DataSource ds = createProxy(DataSource.class, (proxy, method, args) -> {
            if ("getConnection".equals(method.getName())) return conn;
            return null;
        });

        TransactionDAOImpl dao = new TransactionDAOImpl(ds);
        List<Transaction> list = dao.getTransactionsForAccount(5);

        assertEquals(1, list.size());
        Transaction txn = list.get(0);
        assertNull(txn.getFromAccountId(), "from_account_id must be null for deposit, not 0");
        assertEquals(5, txn.getToAccountId());
        assertEquals(TransactionType.DEPOSIT, txn.getType());
        assertEquals(TransactionStatus.COMPLETED, txn.getStatus());
    }

    @Test
    public void testInsertTransactionSetsParametersCorrectly() {
        Map<Integer, Object> params = new HashMap<>();
        PreparedStatement stmt = createProxy(PreparedStatement.class, (proxy, method, args) -> {
            String name = method.getName();
            if ("executeUpdate".equals(name)) return 1;
            if ("setString".equals(name) || "setInt".equals(name) || "setBigDecimal".equals(name) || "setObject".equals(name)) {
                params.put((Integer) args[0], args[1]);
                return null;
            }
            return null;
        });

        Connection conn = createProxy(Connection.class, (proxy, method, args) -> {
            if ("prepareStatement".equals(method.getName())) return stmt;
            return null;
        });

        DataSource ds = createProxy(DataSource.class, (proxy, method, args) -> {
            if ("getConnection".equals(method.getName())) return conn;
            return null;
        });

        TransactionDAOImpl dao = new TransactionDAOImpl(ds);
        Transaction txn = new Transaction(
                null,
                "REF-TEST-99",
                10,
                20,
                new BigDecimal("50.00"),
                TransactionType.TRANSFER,
                TransactionStatus.COMPLETED,
                1,
                new Timestamp(System.currentTimeMillis())
        );

        dao.insert(conn, txn);

        assertEquals("REF-TEST-99", params.get(1));
        assertEquals(10, params.get(2));
        assertEquals(20, params.get(3));
        assertEquals(new BigDecimal("50.00"), params.get(4));
        assertEquals("TRANSFER", params.get(5));
        assertEquals("COMPLETED", params.get(6));
        assertEquals(1, params.get(7));
    }
}
