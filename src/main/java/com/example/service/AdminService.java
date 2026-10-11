package com.example.service;

import com.example.dao.EmployeeDAO;
import com.example.domain.Employee;
import com.example.domain.Role;
import com.example.domain.SessionContext;
import org.mindrot.jbcrypt.BCrypt;

public class AdminService {

    private final EmployeeDAO employeeDAO;

    public AdminService(EmployeeDAO employeeDAO) {
        this.employeeDAO = employeeDAO;
    }

    public void createEmployee(SessionContext session, String username, String rawPassword, Role role, Integer branchId) {
        session.requireRole(Role.ADMIN, Role.MANAGER, Role.BRANCH_MANAGER);

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be blank");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Password cannot be blank");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }
        if (branchId == null) {
            throw new IllegalArgumentException("Branch ID cannot be null");
        }

        String hashedPassword = BCrypt.hashpw(rawPassword, BCrypt.gensalt());
        Employee employee = new Employee(null, branchId, username.trim(), hashedPassword, role);
        employeeDAO.insert(employee);
    }
}