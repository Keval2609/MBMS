package com.example.dao;

import com.example.domain.Customer;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface CustomerDAO {
    Optional<Customer> findById(Integer id);
    Optional<Customer> findById(Connection conn, Integer id);

    Customer insert(Customer customer);
    Customer insert(Connection conn, Customer customer);

    List<Customer> findAll();
    List<Customer> findAll(Connection conn);
}
