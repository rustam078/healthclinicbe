package com.clinic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Shared payload for status changes (appointments, requests, beds, users). */
public record StatusChangeDto(
        @NotBlank(message = "Status is required") String status,
        @Size(max = 255, message = "Reason must be at most 255 characters") String reason) {
}
