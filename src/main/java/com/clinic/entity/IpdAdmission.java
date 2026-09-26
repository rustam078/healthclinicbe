package com.clinic.entity;

import com.clinic.enums.DischargeCondition;
import com.clinic.enums.IpdStatus;
import com.clinic.util.Dates;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "ipd_admissions")
public class IpdAdmission extends BaseEntity {

    @Column(updatable = false)
    private String ipdCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bed_id")
    private Bed bed;

    private LocalDateTime admittedAt;
    private LocalDate expectedDischargeDate;
    private String reason;
    private String notes;

    @Enumerated(EnumType.STRING)
    private IpdStatus status = IpdStatus.ADMITTED;

    private LocalDateTime dischargedAt;

    @Enumerated(EnumType.STRING)
    private DischargeCondition dischargeCondition;

    private String dischargeNotes;
    private String dischargeSummary;

    public long stayDays() {
        return Dates.stayDays(admittedAt, dischargedAt);
    }

    /** Informational estimate (stay days x room daily charge). The clinic does not bill through the system. */
    public BigDecimal estimatedCharges() {
        return bed.getRoom().getDailyCharge().multiply(BigDecimal.valueOf(stayDays()));
    }
}
