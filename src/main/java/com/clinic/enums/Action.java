package com.clinic.enums;

/** Individual actions that can be allowed or denied per role, on top of module read/write access. */
public enum Action {
    APPOINTMENT_COMPLETE,
    APPOINTMENT_APPROVE_DELETE,
    IPD_DISCHARGE,
    PRESCRIPTION_VIEW
}
