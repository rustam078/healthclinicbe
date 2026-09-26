package com.clinic.enums;

public enum Role {
    ADMIN, STAFF;

    public String authority() {
        return "ROLE_" + name();
    }
}
