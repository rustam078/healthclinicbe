package com.clinic.util;

import com.clinic.exception.BusinessException;

/** Implemented by status enums that restrict which status may follow which. */
public interface WorkflowStatus<S extends Enum<S>> {

    boolean canMoveTo(S next);

    /** Parses the requested status and verifies the move is allowed from {@code current}. */
    static <S extends Enum<S> & WorkflowStatus<S>> S transition(S current, Class<S> type, String requested) {
        S next = Enums.require(type, requested);
        if (!current.canMoveTo(next)) {
            throw BusinessException.conflict("Status cannot change from "
                    + Enums.label(current) + " to " + Enums.label(next));
        }
        return next;
    }
}
