package com.example.domain;

import com.example.exception.UnauthorizedAccessException;

public record SessionContext(
    Integer employeeId,
    Integer branchId,
    String username,
    Role role
) {
    public SessionContext(Integer employeeId, Integer branchId, String username, String roleStr) {
        this(employeeId, branchId, username, Role.fromString(roleStr));
    }

    public void requireRole(Role... allowedRoles) {
        if (this.role == null) {
            throw new UnauthorizedAccessException("Unauthenticated session");
        }
        for (Role allowed : allowedRoles) {
            if (this.role.matches(allowed)) {
                return;
            }
        }
        throw new UnauthorizedAccessException("Role '" + this.role + "' is not permitted to perform this operation");
    }
}