package com.clinic.enums;

import com.clinic.dto.TemplateDto;
import com.clinic.entity.Appointment;
import com.clinic.entity.Patient;
import com.clinic.entity.Prescription;

/**
 * Printable documents and their professional defaults (CSS pixels at 96 dpi: A4 = 794 x 1123, A5 = 559 x 794).
 * Used to reset a template or create it when missing.
 */
public enum TemplateType {
    APPOINTMENT_SLIP("Appointment Slip", 559, 794, 110, 160, 56, 24, 16,
            "Please arrive 10 minutes before your appointment time."),
    ADMISSION_FORM("Admission Record", 794, 1123, 130, 180, 60, 32, 24,
            "This is a clinic record. Amounts shown are estimates, not a bill."),
    DISCHARGE_SUMMARY("Discharge Summary", 794, 1123, 130, 180, 60, 32, 24,
            "Follow the advice above and contact the clinic in case of any concern."),
    PATIENT_PROFILE("Patient Profile", 794, 1123, 130, 180, 60, 32, 24,
            "Confidential patient information."),
    REPORT("Clinic Report", 794, 1123, 120, 180, 60, 32, 24,
            "Generated from the clinic management system."),
    PRESCRIPTION("Prescription", 794, 1123, 120, 180, 60, 28, 12,
            "Take medicines as advised. Please bring this prescription on your next visit.");

    private final TemplateDto defaults;

    TemplateType(String title, int width, int height, int header, int logoWidth, int logoHeight,
                 int padding, int margin, String footer) {
        this.defaults = TemplateDto.of(title, new int[]{width, height, header, logoWidth, logoHeight, padding, margin}, footer);
    }

    public TemplateDto defaults() {
        return defaults;
    }
}
