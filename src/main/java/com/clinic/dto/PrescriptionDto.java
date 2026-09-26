package com.clinic.dto;

import com.clinic.entity.Prescription;
import com.clinic.enums.Access;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Prescription ink and follow-up date; {@code editable} tells the pad whether the user may change it. */
@Getter
@Setter
public class PrescriptionDto extends AuditedDto {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long appointmentId;

    /** JSON array of strokes: [{ tool, color, size, points: [[x, y], ...] }, ...] in pad units. */
    @NotNull(message = "Prescription content is required")
    @Size(max = 3_000_000, message = "The prescription is too large")
    private String strokes;

    /** Handwritten weight (same stroke format, in the small "Wt" box). */
    @Size(max = 500_000, message = "The weight entry is too large")
    private String weightStrokes;

    private LocalDate followUpDate;

    /** Last day of free follow-up (from the consultation validity in settings). */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDate validUntil;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime completedAt;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private boolean editable;
}
