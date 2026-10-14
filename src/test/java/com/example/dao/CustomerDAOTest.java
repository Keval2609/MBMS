package com.example.dao;

import com.example.domain.Customer;
import com.example.exception.DataAccessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class CustomerDAOTest {

    @SuppressWarnings("unchecked")
    private <T> T createProxy(Class<T> iface, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, handler);
    }

    @Test
    @DisplayName("Successfully queries customer by ID")
    void testFindByIdSuccess() {
        ResultSet rs = createProxy(ResultSet.class, (proxy, method, args) -> {
            String name = method.getName();
            if ("next".equals(name)) return true;
            if ("getInt".equals(name)) return 101;
            if ("getString".equals(name)) {
                if ("first_name".equals(args[0])) return "Alice";
                if ("last_name".equals(args[0])) return "Smith";
                if ("email".equals(args[0])) return "alice@example.com";
                if ("phone_number".equals(args[0])) return "123-456";
                if ("address".equals(args[0])) return "10 Main St";
                return "";
            }
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

        CustomerDAO dao = new CustomerDAOImpl(ds);
        Optional<Customer> result = dao.findById(101);

        assertTrue(result.isPresent());
        assertEquals(101, result.get().getId());
        assertEquals("Alice", result.get().getFirstName());
        assertEquals("Smith", result.get().getLastName());
        assertEquals("alice@example.com", result.get().getEmail());
    }

    @Test
    @DisplayName("Successfully inserts customer and sets generated ID")
    void testInsertCustomerSuccess() {
        ResultSet genKeys = createProxy(ResultSet.class, (proxy, method, args) -> {
            String name = method.getName();
            if ("next".equals(name)) return true;
            if ("getInt".equals(name)) return 205;
            return null;
        });

        PreparedStatement stmt = createProxy(PreparedStatement.class, (proxy, method, args) -> {
            String name = method.getName();
            if ("executeUpdate".equals(name)) return 1;
            if ("getGeneratedKeys".equals(name)) return genKeys;
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

        CustomerDAO dao = new CustomerDAOImpl(ds);
        Customer customer = new Customer("Bob", "Brown", "bob@example.com", "555-1234", "Central St");
        Customer saved = dao.insert(customer);

        assertNotNull(saved);
        assertEquals(205, saved.getId());
    }

    @Test
    @DisplayName("Throws DataAccessException on SQL failure during insert")
    void testInsertCustomerFailureThrowsDataAccessException() {
        Connection conn = createProxy(Connection.class, (proxy, method, args) -> {
            if ("prepareStatement".equals(method.getName())) {
                throw new SQLException("Unique constraint violation: email exists");
            }
            return null;
        });

        DataSource ds = createProxy(DataSource.class, (proxy, method, args) -> {
            if ("getConnection".equals(method.getName())) return conn;
            return null;
        });

        CustomerDAO dao = new CustomerDAOImpl(ds);
        Customer customer = new Customer("Duplicate", "User", "dup@example.com", "555-9999", "Address");

        assertThrows(DataAccessException.class, () -> dao.insert(customer));
    }
}
