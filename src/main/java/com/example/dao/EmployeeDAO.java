package com.example.dao;

import com.example.domain.Employee;
import java.util.Optional;

public interface EmployeeDAO {
    Optional<Employee> findByUsername(String username);
}