package com.clinic.service;

import com.clinic.dto.PrescriptionDto;
import com.clinic.entity.Appointment;
import com.clinic.entity.Prescription;
import com.clinic.enums.AppointmentStatus;
import com.clinic.exception.BusinessException;
import com.clinic.repository.PrescriptionRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * The doctor's handwritten prescription. Writing it and pressing "Save & complete" completes the appointment.
 * Once completed, only an administrator may change it; everyone else can view and print.
 */
@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionRepository repository;
    private final AppointmentService appointmentService;
    private final ActivityService activityService;

    @Transactional(readOnly = true)
    public PrescriptionDto get(Long appointmentId, boolean canWrite, boolean admin) {
        Appointment appointment = appointmentService.find(appointmentId);
        Prescription prescription = repository.findByAppointmentId(appointmentId).orElseGet(() -> blank(appointment));
        return toDto(prescription, canWrite && (admin || appointment.getStatus() == AppointmentStatus.SCHEDULED));
    }

    /** Auto-save while writing (does not complete the appointment). */
    @Transactional
    public PrescriptionDto save(Long appointmentId, PrescriptionDto dto, boolean admin) {
        Prescription prescription = writable(appointmentId, admin);
        apply(prescription, dto);
        return toDto(repository.save(prescription), true);
    }

    /** Saves the prescription and marks the appointment completed (first time only). */
    @Transactional
    public PrescriptionDto complete(Long appointmentId, PrescriptionDto dto, boolean admin) {
        Prescription prescription = writable(appointmentId, admin);
        apply(prescription, dto);
        boolean firstTime = prescription.getCompletedAt() == null;
        if (firstTime) {
            prescription.setCompletedAt(LocalDateTime.now());
            appointmentService.complete(prescription.getAppointment());
        }
        log(prescription, firstTime ? "Prescription written" : "Prescription updated");
        return toDto(repository.save(prescription), admin);
    }

    private Prescription writable(Long appointmentId, boolean admin) {
        Appointment appointment = appointmentService.find(appointmentId);
        if (appointment.getStatus() == AppointmentStatus.COMPLETED && !admin) {
            throw BusinessException.forbidden("Only an administrator can change the prescription of a completed appointment");
        }
        return repository.findByAppointmentId(appointmentId).orElseGet(() -> blank(appointment));
    }

    private void apply(Prescription prescription, PrescriptionDto dto) {
        validate(dto, prescription.getAppointment());
        prescription.setStrokes(dto.getStrokes());
        prescription.setWeightStrokes(dto.getWeightStrokes() == null ? "[]" : dto.getWeightStrokes());
        prescription.setFollowUpDate(dto.getFollowUpDate());
    }

    private Prescription blank(Appointment appointment) {
        Prescription prescription = new Prescription();
        prescription.setAppointment(appointment);
        prescription.setPatientId(appointment.getPatient().getId());
        return prescription;
    }

    private void log(Prescription prescription, String description) {
        Appointment appointment = prescription.getAppointment();
        activityService.log("APPOINTMENT", appointment.getId(), prescription.getPatientId(), "UPDATED",
                description + " for appointment " + appointment.getAppointmentCode());
    }

    private PrescriptionDto toDto(Prescription prescription, boolean editable) {
        PrescriptionDto dto = new PrescriptionDto();
        dto.setId(prescription.getId());
        dto.setAppointmentId(prescription.getAppointment().getId());
        dto.setStrokes(prescription.getStrokes());
        dto.setWeightStrokes(prescription.getWeightStrokes());
        dto.setFollowUpDate(prescription.getFollowUpDate());
        dto.setCompletedAt(prescription.getCompletedAt());
        dto.setEditable(editable);
        return withMeta(dto, prescription);
    }

    private PrescriptionDto withMeta(PrescriptionDto dto, Prescription prescription) {
        dto.setUpdatedAt(prescription.getUpdatedAt());
        dto.setUpdatedBy(prescription.getUpdatedBy());
        dto.setValidUntil(appointmentService.followUpValidUntil(prescription.getAppointment()));
        return dto;
    }

    private void validate(PrescriptionDto dto, Appointment appointment) {
        if (!dto.getStrokes().stripLeading().startsWith("[")) {
            throw BusinessException.badRequest("Invalid prescription content");
        }
        if (dto.getFollowUpDate() != null && dto.getFollowUpDate().isBefore(appointment.getAppointmentDate())) {
            throw BusinessException.badRequest("Follow-up date cannot be before the visit date");
        }
    }
}
