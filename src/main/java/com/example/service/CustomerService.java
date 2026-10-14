package com.example.service;

import com.example.dao.CustomerDAO;
import com.example.domain.Customer;
import com.example.domain.Role;
import com.example.domain.SessionContext;

import java.util.List;
import java.util.Optional;

public class CustomerService {

    private final CustomerDAO customerDAO;

    public CustomerService(CustomerDAO customerDAO) {
        this.customerDAO = customerDAO;
    }

    public Customer createCustomer(SessionContext session, String firstName, String lastName,
                                  String email, String phoneNumber, String address) {
        session.requireRole(Role.TELLER, Role.MANAGER, Role.BRANCH_MANAGER, Role.ADMIN);

        if (firstName == null || firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }
        if (lastName == null || lastName.trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number is required");
        }

        Customer customer = new Customer(
                firstName.trim(),
                lastName.trim(),
                email.trim(),
                phoneNumber.trim(),
                address != null ? address.trim() : null
        );

        return customerDAO.insert(customer);
    }

    public List<Customer> listCustomers(SessionContext session) {
        session.requireRole(Role.TELLER, Role.MANAGER, Role.BRANCH_MANAGER, Role.ADMIN);
        return customerDAO.findAll();
    }

    public Optional<Customer> getCustomer(SessionContext session, Integer customerId) {
        session.requireRole(Role.TELLER, Role.MANAGER, Role.BRANCH_MANAGER, Role.ADMIN);
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID cannot be null");
        }
        return customerDAO.findById(customerId);
    }
}
