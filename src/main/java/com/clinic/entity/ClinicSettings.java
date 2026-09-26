package com.clinic.entity;

import com.clinic.enums.LogoPosition;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.ZoneId;

/** Single row holding clinic details and branding. The logo file lives on disk; only its path is stored. */
@Getter
@Setter
@Entity
@Table(name = "clinic_settings")
public class ClinicSettings extends BaseEntity {

    private String clinicName;
    private String address;
    private String phone;
    private String email;
    private String website;
    private String registrationNo;
    private String headerText;
    private String footerText;
    private String currencySymbol;
    private String availabilityText;
    private String sidebarMode = "FULL";
    private BigDecimal consultationFee = BigDecimal.ZERO;
    private int followUpValidityDays = 30;
    /** Seconds between automatic list refreshes on every device; 0 = off. */
    private int autoRefreshSeconds = 10;
    private String logoPath;
    private int logoWidth;
    private int logoHeight;

    @Enumerated(EnumType.STRING)
    private LogoPosition logoPosition;

    /** Public URL with a version so browsers reload the image after a change. */
    public String logoUrl() {
        if (logoPath == null) {
            return null;
        }
        long version = getUpdatedAt() == null ? 0 : getUpdatedAt().atZone(ZoneId.systemDefault()).toEpochSecond();
        return "/api/settings/logo?v=" + version;
    }
}
