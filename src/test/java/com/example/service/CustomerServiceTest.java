package com.example.service;

import com.example.dao.CustomerDAO;
import com.example.domain.Customer;
import com.example.domain.Role;
import com.example.domain.SessionContext;
import com.example.exception.UnauthorizedAccessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CustomerServiceTest {

    private final SessionContext tellerSession = new SessionContext(1, 1, "teller", Role.TELLER);
    private final SessionContext unauthSession = new SessionContext(2, 1, "anonymous", (Role) null);

    @SuppressWarnings("unchecked")
    private <T> T createProxy(Class<T> iface, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, handler);
    }

    @Test
    @DisplayName("Successfully creates customer with trimmed fields")
    void testCreateCustomerSuccess() {
        AtomicReference<Customer> savedCustomer = new AtomicReference<>();
        CustomerDAO customerDAO = createProxy(CustomerDAO.class, (proxy, method, args) -> {
            if ("insert".equals(method.getName())) {
                Customer c = (Customer) args[0];
                c.setId(10);
                savedCustomer.set(c);
                return c;
            }
            return null;
        });

        CustomerService service = new CustomerService(customerDAO);
        Customer created = service.createCustomer(tellerSession, "  Alice ", " Smith ", " alice@example.com ", " 555-0100 ", " 123 Main St ");

        assertNotNull(created);
        assertEquals(10, created.getId());
        assertEquals("Alice", created.getFirstName());
        assertEquals("Smith", created.getLastName());
        assertEquals("alice@example.com", created.getEmail());
        assertEquals("555-0100", created.getPhoneNumber());
        assertEquals("123 Main St", created.getAddress());
    }

    @Test
    @DisplayName("Rejects missing required fields when creating customer")
    void testCreateCustomerValidationErrors() {
        CustomerDAO customerDAO = createProxy(CustomerDAO.class, (proxy, method, args) -> null);
        CustomerService service = new CustomerService(customerDAO);

        assertThrows(IllegalArgumentException.class, () ->
                service.createCustomer(tellerSession, "", "Smith", "a@b.com", "555", "addr"));
        assertThrows(IllegalArgumentException.class, () ->
                service.createCustomer(tellerSession, "Alice", "", "a@b.com", "555", "addr"));
        assertThrows(IllegalArgumentException.class, () ->
                service.createCustomer(tellerSession, "Alice", "Smith", "", "555", "addr"));
        assertThrows(IllegalArgumentException.class, () ->
                service.createCustomer(tellerSession, "Alice", "Smith", "a@b.com", "", "addr"));
    }

    @Test
    @DisplayName("Rejects unauthenticated session when creating customer")
    void testCreateCustomerRejectsUnauthorized() {
        CustomerService service = new CustomerService(null);
        assertThrows(UnauthorizedAccessException.class, () ->
                service.createCustomer(unauthSession, "A", "B", "a@b.com", "555", "addr"));
    }

    @Test
    @DisplayName("Lists customers successfully")
    void testListCustomers() {
        List<Customer> list = new ArrayList<>();
        list.add(new Customer(1, "A", "B", "a@b.com", "111", "addr1", null));
        list.add(new Customer(2, "C", "D", "c@d.com", "222", "addr2", null));

        CustomerDAO customerDAO = createProxy(CustomerDAO.class, (proxy, method, args) -> {
            if ("findAll".equals(method.getName())) return list;
            return null;
        });

        CustomerService service = new CustomerService(customerDAO);
        List<Customer> result = service.listCustomers(tellerSession);

        assertEquals(2, result.size());
        assertEquals("A", result.get(0).getFirstName());
    }
}
