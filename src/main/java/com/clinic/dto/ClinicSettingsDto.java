package com.clinic.dto;

import com.clinic.enums.Access;
import com.clinic.enums.LogoPosition;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ClinicSettingsDto extends AuditedDto {

    @NotBlank(message = "Clinic name is required")
    @Size(max = 120, message = "Clinic name must be at most 120 characters")
    private String clinicName;

    @Size(max = 255, message = "Address must be at most 255 characters")
    private String address;

    @Size(max = 30, message = "Phone must be at most 30 characters")
    private String phone;

    @Email(message = "Enter a valid email address")
    @Size(max = 120, message = "Email must be at most 120 characters")
    private String email;

    @Size(max = 120, message = "Website must be at most 120 characters")
    private String website;

    @Size(max = 60, message = "Registration number must be at most 60 characters")
    private String registrationNo;

    @Size(max = 255, message = "Header text must be at most 255 characters")
    private String headerText;

    @Size(max = 255, message = "Footer text must be at most 255 characters")
    private String footerText;

    /** Short highlight shown in the prescription header, e.g. "24×7". */
    @Size(max = 60, message = "Availability text must be at most 60 characters")
    private String availabilityText;

    /** Default left menu: FULL (icons and names) or ICONS (icons only). */
    @Pattern(regexp = "FULL|ICONS", message = "Menu style must be FULL or ICONS")
    private String sidebarMode;

    @NotBlank(message = "Currency symbol is required")
    @Size(max = 5, message = "Currency symbol must be at most 5 characters")
    private String currencySymbol;

    @NotNull(message = "Consultation fee is required (use 0 if none)")
    @PositiveOrZero(message = "Consultation fee cannot be negative")
    @DecimalMax(value = "99999999.99", message = "Consultation fee is too large")
    private BigDecimal consultationFee;

    /** A follow-up is free when it falls within this many days of the last completed consultation. */
    @NotNull(message = "Follow-up validity is required")
    @Min(value = 0, message = "Follow-up validity cannot be negative")
    @Max(value = 365, message = "Follow-up validity must be at most 365 days")
    private Integer followUpValidityDays;

    @NotNull(message = "Logo width is required")
    @Min(value = 20, message = "Logo width must be at least 20 px")
    @Max(value = 600, message = "Logo width must be at most 600 px")
    private Integer logoWidth;

    @NotNull(message = "Logo height is required")
    @Min(value = 20, message = "Logo height must be at least 20 px")
    @Max(value = 300, message = "Logo height must be at most 300 px")
    private Integer logoHeight;

    @NotNull(message = "Logo position is required")
    private LogoPosition logoPosition;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String logoUrl;
}
