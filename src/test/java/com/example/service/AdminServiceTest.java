package com.example.service;

import com.example.dao.EmployeeDAO;
import com.example.domain.Employee;
import com.example.domain.Role;
import com.example.domain.SessionContext;
import com.example.exception.UnauthorizedAccessException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class AdminServiceTest {

    private final SessionContext adminSession = new SessionContext(1, 1, "admin", Role.ADMIN);
    private final SessionContext tellerSession = new SessionContext(2, 1, "teller", Role.TELLER);

    @SuppressWarnings("unchecked")
    private <T> T createProxy(Class<T> iface, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, handler);
    }

    @Test
    public void testCreateEmployeeSuccess() {
        AtomicReference<Employee> savedEmployee = new AtomicReference<>();
        EmployeeDAO dao = createProxy(EmployeeDAO.class, (proxy, method, args) -> {
            if ("insert".equals(method.getName())) {
                savedEmployee.set((Employee) args[0]);
                return null;
            }
            return null;
        });

        AdminService adminService = new AdminService(dao);
        adminService.createEmployee(adminSession, "teller_bob", "p@ssword", Role.TELLER, 2);

        assertNotNull(savedEmployee.get());
        assertEquals("teller_bob", savedEmployee.get().getUsername());
        assertEquals(Role.TELLER, savedEmployee.get().getRole());
        assertEquals(2, savedEmployee.get().getBranchId());
        assertTrue(org.mindrot.jbcrypt.BCrypt.checkpw("p@ssword", savedEmployee.get().getPasswordHash()));
    }

    @Test
    public void testCreateEmployeeFailsWhenNotAdminOrManager() {
        AdminService adminService = new AdminService(null);

        assertThrows(UnauthorizedAccessException.class, () ->
            adminService.createEmployee(tellerSession, "bob", "pass", Role.TELLER, 1)
        );
    }
}
