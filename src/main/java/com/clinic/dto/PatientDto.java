package com.clinic.dto;

import com.clinic.enums.Access;
import com.clinic.enums.Gender;
import com.clinic.enums.PatientStatus;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class PatientDto extends AuditedDto {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String patientCode;

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    private String fullName;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = Patterns.PHONE, message = Patterns.PHONE_MESSAGE)
    private String phone;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer age;

    /** e.g. "5 months 18 days" or "1 year 6 months". */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String ageText;

    @Size(max = 255, message = "Address must be at most 255 characters")
    private String address;

    @Size(max = 100, message = "Emergency contact name must be at most 100 characters")
    private String emergencyContactName;

    @Pattern(regexp = Patterns.OPTIONAL_PHONE, message = Patterns.PHONE_MESSAGE)
    private String emergencyContactPhone;

    @Pattern(regexp = Patterns.BLOOD_GROUP, message = "Blood group must be like A+, O- or AB+")
    private String bloodGroup;

    @Size(max = 255, message = "Allergies must be at most 255 characters")
    private String allergies;

    private PatientStatus status;
}
