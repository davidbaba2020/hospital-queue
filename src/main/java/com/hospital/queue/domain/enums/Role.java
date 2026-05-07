package com.hospital.queue.domain.enums;

public enum Role {
    ADMIN,
    DOCTOR,
    RECEPTIONIST,
    PATIENT;

    public String authority() {
        return "ROLE_" + this.name();
    }
}
