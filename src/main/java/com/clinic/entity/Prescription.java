package com.clinic.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Handwritten prescription for one appointment, stored as vector ink strokes (JSON). */
@Getter
@Setter
@Entity
@Table(name = "prescriptions")
public class Prescription extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    private Long patientId;
    private String strokes = "[]";
    private String weightStrokes = "[]";
    private LocalDate followUpDate;
    private LocalDateTime completedAt;
}
