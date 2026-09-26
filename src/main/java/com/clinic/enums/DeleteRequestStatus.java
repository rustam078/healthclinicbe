package com.clinic.enums;

import com.clinic.util.WorkflowStatus;

public enum DeleteRequestStatus implements WorkflowStatus<DeleteRequestStatus> {
    PENDING, APPROVED, REJECTED;

    @Override
    public boolean canMoveTo(DeleteRequestStatus next) {
        return this == PENDING && next != PENDING;
    }
}
