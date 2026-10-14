package com.example.domain;

public class Employee {
    private Integer id;
    private Integer branchId;
    private String username;
    private String passwordHash;
    private Role role;

    public Employee() {}

    public Employee(Integer id, Integer branchId, String username, String passwordHash, Role role) {
        this.id = id;
        this.branchId = branchId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public Integer getBranchId() { return branchId; }
    public void setBranchId(Integer branchId) { this.branchId = branchId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public void setRole(String role) { this.role = Role.fromString(role); }
}