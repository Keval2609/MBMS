package com.example.dao;

import com.example.domain.Employee;
import java.sql.Connection;
import java.util.Optional;

public interface EmployeeDAO {
    Optional<Employee> findByUsername(String username);
    Optional<Employee> findByUsername(Connection conn, String username);
    void insert(Employee employee);
    void insert(Connection conn, Employee employee);
}