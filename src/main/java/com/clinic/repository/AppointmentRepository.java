package com.clinic.repository;

import com.clinic.entity.Appointment;
import com.clinic.enums.AppointmentStatus;
import com.clinic.enums.AppointmentType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface AppointmentRepository extends BaseRepository<Appointment> {

    @Override
    @EntityGraph(attributePaths = {"patient", "doctor"})
    Page<Appointment> findAll(Specification<Appointment> spec, Pageable pageable);

    @Query("select coalesce(max(a.tokenNumber), 0) from Appointment a where a.appointmentDate = :date")
    int maxToken(@Param("date") LocalDate date);

    Optional<Appointment> findFirstByPatientIdAndStatusAndTypeOrderByAppointmentDateDesc(
            Long patientId, AppointmentStatus status, AppointmentType type);

    Optional<Appointment> findFirstByPatientIdAndAppointmentDateAndStatus(
            Long patientId, LocalDate date, AppointmentStatus status);
}
