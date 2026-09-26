package com.clinic.dto;

/** Validation regular expressions shared by DTOs. Optional variants also accept an empty string. */
public final class Patterns {

    public static final String PHONE = "^[0-9+()\\-\\s]{7,20}$";
    public static final String OPTIONAL_PHONE = "^$|" + PHONE;
    public static final String BLOOD_GROUP = "^$|^(A|B|AB|O)[+-]$";
    public static final String HEX_COLOR = "^#[0-9a-fA-F]{6}$";
    public static final String USERNAME = "^[a-zA-Z0-9._-]{3,50}$";

    public static final String PHONE_MESSAGE = "Enter a valid phone number (7-20 digits)";

    private Patterns() {
    }
}
