package com.example.service;

import com.example.dao.EmployeeDAO;
import com.example.domain.Employee;
import com.example.domain.Role;
import com.example.domain.SessionContext;
import com.example.exception.DataAccessException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class AuthServiceTest {

    @SuppressWarnings("unchecked")
    private <T> T createProxy(Class<T> iface, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, handler);
    }

    @Test
    public void testLoginSuccessful() {
        String hashed = org.mindrot.jbcrypt.BCrypt.hashpw("secret123", org.mindrot.jbcrypt.BCrypt.gensalt());
        Employee emp = new Employee(1, 10, "admin", hashed, Role.ADMIN);

        EmployeeDAO dao = createProxy(EmployeeDAO.class, (proxy, method, args) -> {
            if ("findByUsername".equals(method.getName())) {
                return Optional.of(emp);
            }
            return Optional.empty();
        });

        AuthService service = new AuthService(dao);
        Optional<SessionContext> session = service.login("admin", "secret123");

        assertTrue(session.isPresent());
        assertEquals("admin", session.get().username());
        assertEquals(Role.ADMIN, session.get().role());
        assertEquals(1, session.get().employeeId());
        assertEquals(10, session.get().branchId());
    }

    @Test
    public void testLoginFailsOnBadPassword() {
        String hashed = org.mindrot.jbcrypt.BCrypt.hashpw("secret123", org.mindrot.jbcrypt.BCrypt.gensalt());
        Employee emp = new Employee(1, 10, "admin", hashed, Role.ADMIN);

        EmployeeDAO dao = createProxy(EmployeeDAO.class, (proxy, method, args) -> {
            if ("findByUsername".equals(method.getName())) {
                return Optional.of(emp);
            }
            return Optional.empty();
        });

        AuthService service = new AuthService(dao);
        Optional<SessionContext> session = service.login("admin", "wrongpass");

        assertTrue(session.isEmpty());
    }

    @Test
    public void testLoginPropagatesDataAccessExceptionOnDatabaseOutage() {
        EmployeeDAO dao = createProxy(EmployeeDAO.class, (proxy, method, args) -> {
            if ("findByUsername".equals(method.getName())) {
                throw new DataAccessException("Connection failed", new SQLException("Connection refused"));
            }
            return null;
        });

        AuthService service = new AuthService(dao);
        DataAccessException ex = assertThrows(DataAccessException.class, () ->
            service.login("admin", "secret")
        );
        assertEquals("Connection failed", ex.getMessage());
    }
}
