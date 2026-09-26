package com.clinic.entity;

import com.clinic.enums.DeleteRequestStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A request to remove a scheduled booking. It keeps a snapshot of the appointment,
 * because the appointment row itself is deleted once the request is approved.
 */
@Getter
@Setter
@Entity
@Table(name = "appointment_delete_requests")
public class AppointmentDeleteRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    private String appointmentCode;
    private Long patientId;
    private String patientName;
    private String patientPhone;
    private LocalDate appointmentDate;
    private Integer tokenNumber;

    @Enumerated(EnumType.STRING)
    private DeleteRequestStatus status = DeleteRequestStatus.PENDING;

    private String decidedBy;
    private LocalDateTime decidedAt;
}
