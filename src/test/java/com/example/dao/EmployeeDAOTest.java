package com.example.dao;

import com.example.domain.Employee;
import com.example.domain.Role;
import com.example.exception.DataAccessException;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class EmployeeDAOTest {

    @SuppressWarnings("unchecked")
    private <T> T createProxy(Class<T> iface, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, handler);
    }

    @Test
    public void testFindByUsernameSuccessful() {
        ResultSet rs = createProxy(ResultSet.class, (proxy, method, args) -> {
            String name = method.getName();
            if ("next".equals(name)) return true;
            if ("getInt".equals(name)) {
                if ("id".equals(args[0])) return 1;
                if ("branch_id".equals(args[0])) return 10;
                return 0;
            }
            if ("getString".equals(name)) {
                if ("username".equals(args[0])) return "admin";
                if ("password_hash".equals(args[0])) return "$2a$10$hash";
                if ("role".equals(args[0])) return "ADMIN";
                return "";
            }
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

        EmployeeDAOImpl dao = new EmployeeDAOImpl(ds);
        Optional<Employee> emp = dao.findByUsername("admin");

        assertTrue(emp.isPresent());
        assertEquals("admin", emp.get().getUsername());
        assertEquals(Role.ADMIN, emp.get().getRole());
        assertEquals(10, emp.get().getBranchId());
    }

    @Test
    public void testFindByUsernameThrowsDataAccessExceptionOnSqlError() {
        Connection conn = createProxy(Connection.class, (proxy, method, args) -> {
            if ("prepareStatement".equals(method.getName())) {
                throw new SQLException("DB offline");
            }
            return null;
        });

        DataSource ds = createProxy(DataSource.class, (proxy, method, args) -> {
            if ("getConnection".equals(method.getName())) return conn;
            return null;
        });

        EmployeeDAOImpl dao = new EmployeeDAOImpl(ds);
        DataAccessException ex = assertThrows(DataAccessException.class, () -> dao.findByUsername("admin"));
        assertTrue(ex.getMessage().contains("admin"));
        assertInstanceOf(SQLException.class, ex.getCause());
    }

    @Test
    public void testInsertEmployeeExecutesSuccessfully() {
        Map<Integer, Object> params = new HashMap<>();
        PreparedStatement stmt = createProxy(PreparedStatement.class, (proxy, method, args) -> {
            if ("executeUpdate".equals(method.getName())) return 1;
            if ("setString".equals(method.getName()) || "setInt".equals(method.getName())) {
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

        EmployeeDAOImpl dao = new EmployeeDAOImpl(ds);
        Employee emp = new Employee(null, 1, "teller1", "secretHash", Role.TELLER);
        dao.insert(conn, emp);

        assertEquals(1, params.get(1));
        assertEquals("teller1", params.get(2));
        assertEquals("secretHash", params.get(3));
        assertEquals("TELLER", params.get(4));
    }
}
