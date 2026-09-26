package com.clinic.enums;

import com.clinic.util.WorkflowStatus;

/** OCCUPIED is set and cleared only by admission/discharge, never manually. */
public enum BedStatus implements WorkflowStatus<BedStatus> {
    AVAILABLE, OCCUPIED, RESERVED, MAINTENANCE;

    @Override
    public boolean canMoveTo(BedStatus next) {
        return this != OCCUPIED && next != OCCUPIED && next != this;
    }

    public boolean isAssignable() {
        return this == AVAILABLE || this == RESERVED;
    }
}
