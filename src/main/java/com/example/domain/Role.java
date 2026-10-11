package com.example.domain;

public enum Role {
    ADMIN,
    MANAGER,
    BRANCH_MANAGER,
    TELLER;

    public static Role fromString(String value) {
        if (value == null) return null;
        String normalized = value.trim().toUpperCase();
        if ("BRANCH_MANAGER".equals(normalized)) {
            return BRANCH_MANAGER;
        }
        return Role.valueOf(normalized);
    }

    public boolean matches(Role other) {
        if (this == other) return true;
        if ((this == MANAGER || this == BRANCH_MANAGER) && (other == MANAGER || other == BRANCH_MANAGER)) {
            return true;
        }
        return false;
    }
}
