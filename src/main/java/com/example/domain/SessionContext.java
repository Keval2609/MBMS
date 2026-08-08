package com.example.domain;

public record SessionContext(
    Integer employeeId,
    Integer branchId,
    String username,
    String role 
) {}