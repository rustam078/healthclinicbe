package com.clinic.dto;

import com.clinic.dto.ValidationGroups.OnDischarge;
import com.clinic.entity.Bed;
import com.clinic.entity.Patient;
import com.clinic.enums.Access;
import com.clinic.enums.DischargeCondition;
import com.clinic.enums.Gender;
import com.clinic.enums.IpdStatus;
import com.clinic.enums.RoomType;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Used for admission, edits and (with the OnDischarge group) the discharge request. */
@Getter
@Setter
public class IpdAdmissionDto extends AuditedDto {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String ipdCode;

    @NotNull(message = "Patient is required")
    private Long patientId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String patientName;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String patientCode;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String patientPhone;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Gender patientGender;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer patientAge;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String patientAgeText;

    /** Set automatically to the clinic doctor. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long doctorId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String doctorName;

    @NotNull(message = "Bed is required")
    private Long bedId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String bedNumber;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String roomNumber;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private RoomType roomType;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private BigDecimal dailyCharge;

    @NotNull(message = "Admission date and time is required")
    private LocalDateTime admittedAt;

    private LocalDate expectedDischargeDate;

    @NotBlank(message = "Reason for admission is required")
    @Size(max = 255, message = "Reason must be at most 255 characters")
    private String reason;

    @Size(max = 1000, message = "Notes must be at most 1000 characters")
    private String notes;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private IpdStatus status;

    @NotNull(groups = OnDischarge.class, message = "Discharge date and time is required")
    private LocalDateTime dischargedAt;

    @NotNull(groups = OnDischarge.class, message = "Condition at discharge is required")
    private DischargeCondition dischargeCondition;

    @Size(groups = OnDischarge.class, max = 1000, message = "Discharge notes must be at most 1000 characters")
    private String dischargeNotes;

    @NotBlank(groups = OnDischarge.class, message = "Discharge summary is required")
    @Size(groups = OnDischarge.class, max = 2000, message = "Discharge summary must be at most 2000 characters")
    private String dischargeSummary;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long stayDays;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private BigDecimal estimatedCharges;
}
