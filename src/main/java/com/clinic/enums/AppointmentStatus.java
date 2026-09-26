package com.clinic.enums;

import com.clinic.util.WorkflowStatus;

/** Walk-in queue: a booking waits as Scheduled until the doctor has seen the patient. */
public enum AppointmentStatus implements WorkflowStatus<AppointmentStatus> {
    SCHEDULED, COMPLETED;

    @Override
    public boolean canMoveTo(AppointmentStatus next) {
        return this == SCHEDULED && next == COMPLETED;
    }
}
