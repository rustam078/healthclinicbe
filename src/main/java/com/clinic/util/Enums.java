package com.clinic.util;

import com.clinic.exception.BusinessException;

import java.util.Arrays;

/** Safe conversion of request strings (filters, status changes) into enum constants. */
public final class Enums {

    private Enums() {
    }

    public static <E extends Enum<E>> E parse(Class<E> type, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Arrays.stream(type.getEnumConstants())
                .filter(constant -> constant.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> BusinessException.badRequest("Invalid value: " + value));
    }

    /** NO_SHOW -> "No show" for human readable messages. */
    public static String label(Enum<?> value) {
        String text = value.name().replace('_', ' ').toLowerCase();
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    public static <E extends Enum<E>> E require(Class<E> type, String value) {
        E parsed = parse(type, value);
        if (parsed == null) {
            throw BusinessException.badRequest("A value is required");
        }
        return parsed;
    }
}
