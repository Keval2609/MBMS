package com.example.service;

import com.example.dao.EmployeeDAO;
import com.example.domain.Employee;
import com.example.domain.SessionContext;
import org.mindrot.jbcrypt.BCrypt;
import java.util.Optional;

public class AuthService {

    private final EmployeeDAO employeeDAO;

    public AuthService(EmployeeDAO employeeDAO) {
        this.employeeDAO = employeeDAO;
    }

    public Optional<SessionContext> login(String username, String rawPassword) {
        // 1. Fetch the employee from the database
        Optional<Employee> employeeOpt = employeeDAO.findByUsername(username);

        // 2. If the user doesn't exist, login fails
        if (employeeOpt.isEmpty()) {
            return Optional.empty();
        }

        Employee employee = employeeOpt.get();

        // 3. Verify the password using BCrypt
        if (BCrypt.checkpw(rawPassword, employee.getPasswordHash())) {
            
            // 4. Success! Create and return their Session badge
            SessionContext session = new SessionContext(
                employee.getId(),
                employee.getBranchId(),
                employee.getUsername(),
                employee.getRole()
            );
            return Optional.of(session);
        }

        // Passwords didn't match
        return Optional.empty();
    }
}