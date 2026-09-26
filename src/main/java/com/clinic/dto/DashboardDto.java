package com.clinic.dto;

import com.clinic.entity.Appointment;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Clinic overview. Built only from data the modules already expose; detailed report numbers
 * come from the Appointment and IPD report endpoints.
 *
 * @param counts            headline numbers (TODAY_APPOINTMENTS, UPCOMING_APPOINTMENTS, PENDING_REQUESTS,
 *                          CURRENTLY_ADMITTED, ADMISSIONS_IN_PERIOD, DISCHARGES_IN_PERIOD)
 * @param appointmentStatus appointment counts by status in the period, plus TOTAL
 * @param appointmentTrend  appointments per day in the period
 * @param beds              beds by status right now, plus TOTAL
 */
public record DashboardDto(LocalDate from,
                           LocalDate to,
                           Map<String, Long> counts,
                           Map<String, Long> appointmentStatus,
                           Map<String, Long> appointmentTrend,
                           Map<String, Long> beds,
                           List<AppointmentDto> todaySchedule,
                           List<IpdAdmissionDto> currentAdmissions,
                           List<PatientDto> recentPatients) {
}
