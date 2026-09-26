package com.clinic.dto;

import com.clinic.dto.ValidationGroups.OnCreate;
import com.clinic.entity.Appointment;
import com.clinic.enums.Access;
import com.clinic.enums.AppointmentStatus;
import com.clinic.enums.AppointmentType;
import com.clinic.enums.Gender;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Booking request and appointment view. To book, send either {@code patientId} (existing patient, optionally
 * with corrected {@code patient} details) or only {@code patient} (a new patient). Everything else is decided
 * by the server: token, visit type, fee, doctor and status.
 */
@Getter
@Setter
public class AppointmentDto extends AuditedDto {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String appointmentCode;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer tokenNumber;

    private Long patientId;

    @Valid
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private PatientDto patient;

    /** Defaults to today. */
    @FutureOrPresent(groups = OnCreate.class, message = "Appointment date cannot be in the past")
    private LocalDate appointmentDate;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalTime appointmentTime;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private AppointmentType type;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private AppointmentStatus status;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private BigDecimal fee;

    /** Last day a free follow-up is possible (set on booking previews). */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDate followUpValidUntil;

    /** True while a delete request for this appointment is waiting for approval. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private boolean deletePending;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String patientName;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String patientCode;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String patientPhone;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Gender patientGender;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDate patientDateOfBirth;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String patientAgeText;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String patientAddress;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String doctorName;
}
