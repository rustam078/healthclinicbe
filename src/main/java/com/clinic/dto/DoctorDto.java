package com.clinic.dto;

import com.clinic.entity.Doctor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DoctorDto extends AuditedDto {

    @NotBlank(message = "Doctor name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    private String fullName;

    /** Qualification / specialization shown on documents, e.g. "MBBS, MD (Paediatrics)". */
    @Size(max = 100, message = "Qualification must be at most 100 characters")
    private String specialization;

    @Pattern(regexp = Patterns.OPTIONAL_PHONE, message = Patterns.PHONE_MESSAGE)
    private String phone;

    @Email(message = "Enter a valid email address")
    @Size(max = 120, message = "Email must be at most 120 characters")
    private String email;

    /** Shown under the doctor on the prescription, e.g. "NICU, CPAP, Emergency & Vaccination". */
    @Size(max = 255, message = "Facilities must be at most 255 characters")
    private String facilities;

    /** Saved handwritten signature (stroke JSON) printed on every prescription. */
    @Size(max = 300_000, message = "The signature is too large")
    private String signature;

    private Boolean active;
}
