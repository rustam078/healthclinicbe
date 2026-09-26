package com.clinic.service;

import com.clinic.dto.AppointmentDto;
import com.clinic.dto.DashboardDto;
import com.clinic.dto.IpdAdmissionDto;
import com.clinic.dto.PatientDto;
import com.clinic.dto.SearchFilter;
import com.clinic.entity.Appointment;
import com.clinic.entity.Bed;
import com.clinic.entity.IpdAdmission;
import com.clinic.enums.AppointmentStatus;
import com.clinic.enums.DeleteRequestStatus;
import com.clinic.enums.IpdStatus;
import com.clinic.repository.AppointmentRepository;
import com.clinic.repository.DeleteRequestRepository;
import com.clinic.repository.IpdAdmissionRepository;
import com.clinic.repository.ReportQueries;
import com.clinic.repository.Specs;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Aggregates the overview shown on the Dashboard from the existing module services and repositories. */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int LIST_SIZE = 6;

    private final AppointmentRepository appointmentRepository;
    private final DeleteRequestRepository deleteRequestRepository;
    private final IpdAdmissionRepository admissionRepository;
    private final AppointmentService appointmentService;
    private final IpdService ipdService;
    private final PatientService patientService;
    private final ReportQueries reportQueries;

    @Transactional(readOnly = true)
    public DashboardDto overview(LocalDate from, LocalDate to) {
        LocalDate start = from == null ? LocalDate.now() : from;
        LocalDate end = to == null ? start : to;
        Specification<Appointment> period = Specs.all(Specs.notDeleted(), Specs.dateBetween("appointmentDate", start, end));
        return new DashboardDto(start, end, counts(start, end),
                reportQueries.countWithTotal(Appointment.class, period, "status"),
                reportQueries.countBy(Appointment.class, period, "appointmentDate"),
                bedStatus(), todaySchedule(), currentAdmissions(), recentPatients());
    }

    private Map<String, Long> counts(LocalDate start, LocalDate end) {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("TODAY_APPOINTMENTS", appointmentRepository.count(appointmentsOn(LocalDate.now())));
        counts.put("UPCOMING_APPOINTMENTS", appointmentRepository.count(upcomingAppointments()));
        counts.put("PENDING_DELETE_REQUESTS", deleteRequestRepository.countByStatusAndDeletedFalse(DeleteRequestStatus.PENDING));
        counts.put("CURRENTLY_ADMITTED", admissionRepository.countByStatusAndDeletedFalse(IpdStatus.ADMITTED));
        counts.put("ADMISSIONS_IN_PERIOD", admissionRepository.count(admissionSpec("admittedAt", start, end)));
        counts.put("DISCHARGES_IN_PERIOD", admissionRepository.count(admissionSpec("dischargedAt", start, end)));
        return counts;
    }

    private Specification<Appointment> appointmentsOn(LocalDate day) {
        return Specs.all(Specs.notDeleted(), Specs.eq("appointmentDate", day));
    }

    private Specification<Appointment> upcomingAppointments() {
        return Specs.all(Specs.notDeleted(), Specs.eq("status", AppointmentStatus.SCHEDULED),
                (root, query, cb) -> cb.greaterThan(root.get("appointmentDate"), LocalDate.now()));
    }

    private Specification<IpdAdmission> admissionSpec(String field, LocalDate start, LocalDate end) {
        return Specs.all(Specs.notDeleted(), Specs.dateTimeBetween(field, start, end));
    }

    /** Beds in active, non-deleted rooms, counted by status. */
    private Map<String, Long> bedStatus() {
        Specification<Bed> beds = Specs.all(Specs.notDeleted(), Specs.eq("room.deleted", false), Specs.eq("room.active", true));
        return reportQueries.countWithTotal(Bed.class, beds, "status");
    }

    private List<AppointmentDto> todaySchedule() {
        SearchFilter filter = listFilter(null);
        filter.setFrom(LocalDate.now());
        filter.setTo(LocalDate.now());
        filter.setSize(100);
        return appointmentService.page(filter).content();
    }

    private List<IpdAdmissionDto> currentAdmissions() {
        SearchFilter filter = listFilter("admittedAt,desc");
        filter.setStatus(IpdStatus.ADMITTED.name());
        return ipdService.page(filter).content();
    }

    private List<PatientDto> recentPatients() {
        return patientService.page(listFilter("createdAt,desc")).content();
    }

    private SearchFilter listFilter(String sort) {
        SearchFilter filter = new SearchFilter();
        filter.setSize(LIST_SIZE);
        filter.setSort(sort);
        return filter;
    }
}
