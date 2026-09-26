package com.clinic.exception;

import java.util.Map;

/** Translates database constraint names into messages clinic staff can understand. */
final class ConstraintMessages {

    private static final String FALLBACK = "This record conflicts with existing data";

    private static final Map<String, String> MESSAGES = Map.of(
            "uq_appointment_token", "Another booking was saved at the same moment. Please try again",
            "uq_delete_request_pending", "A delete request for this appointment is already waiting for approval",
            "uq_ipd_active_bed", "This bed is already occupied by another patient",
            "uq_ipd_active_patient", "This patient is already admitted",
            "uq_room_number", "A room with this number already exists",
            "uq_bed_number", "A bed with this number already exists in the room",
            "app_users_username_key", "This username is already taken",
            "uq_role_module", "Permission for this role and module already exists");

    private ConstraintMessages() {
    }

    static String resolve(String rootMessage) {
        if (rootMessage == null) {
            return FALLBACK;
        }
        return MESSAGES.entrySet().stream()
                .filter(entry -> rootMessage.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(FALLBACK);
    }
}
