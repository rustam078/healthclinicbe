package com.clinic.service;

import com.clinic.dto.AppointmentDto;
import com.clinic.dto.ClinicSettingsDto;
import com.clinic.dto.DeleteRequestDto;
import com.clinic.dto.PageResponse;
import com.clinic.dto.ReportDto;
import com.clinic.dto.SearchFilter;
import com.clinic.dto.StatusChangeDto;
import com.clinic.entity.Appointment;
import com.clinic.entity.AppointmentDeleteRequest;
import com.clinic.enums.AppointmentStatus;
import com.clinic.enums.AppointmentType;
import com.clinic.enums.DeleteRequestStatus;
import com.clinic.exception.BusinessException;
import com.clinic.mapper.AppointmentMapper;
import com.clinic.repository.AppointmentRepository;
import com.clinic.repository.DeleteRequestRepository;
import com.clinic.repository.ReportQueries;
import com.clinic.repository.Specs;
import com.clinic.util.CodeGenerator;
import com.clinic.util.CodeGenerator.CodeType;
import com.clinic.util.CurrentUser;
import com.clinic.util.Dates;
import com.clinic.util.Enums;
import com.clinic.util.WorkflowStatus;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Appointment module: the walk-in queue (token order), automatic visit type and fee,
 * reports, and delete requests (staff ask, an authorised user approves and the booking is removed).
 */
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private static final String TYPE = "APPOINTMENT";
    private static final String REQUEST_TYPE = "DELETE_REQUEST";

    private final AppointmentRepository repository;
    private final DeleteRequestRepository requestRepository;
    private final AppointmentMapper mapper;
    private final CrudSupport crud;
    private final PatientService patientService;
    private final SettingsService settingsService;
    private final ActivityService activityService;
    private final CodeGenerator codeGenerator;
    private final ReportQueries reportQueries;

    // ---- queue ----------------------------------------------------------------------

    /** Without an explicit sort the list is the queue: waiting patients first by token, completed ones last. */
    @Transactional(readOnly = true)
    public PageResponse<AppointmentDto> page(SearchFilter filter) {
        Specification<Appointment> spec = filter.hasSort() ? filterSpec(filter) : Specs.all(filterSpec(filter), queueOrder());
        var pageable = filter.hasSort() ? filter.toPageable("appointmentDate,desc") : filter.toUnsortedPageable();
        return markDeletePending(crud.page(repository, spec, pageable, mapper::toDto));
    }

    @Transactional(readOnly = true)
    public AppointmentDto get(Long id) {
        AppointmentDto dto = mapper.toDto(find(id));
        dto.setDeletePending(requestRepository.existsByAppointmentIdAndStatus(id, DeleteRequestStatus.PENDING));
        return dto;
    }

    public Appointment find(Long id) {
        return crud.find(repository, id, "Appointment");
    }

    // ---- booking ----------------------------------------------------------------------

    /** Books a walk-in: patient (new or existing), clinic doctor, next token, automatic type and fee. */
    @Transactional
    public AppointmentDto book(AppointmentDto dto) {
        Appointment appointment = mapper.toEntity(dto);
        appointment.setPatient(patientService.forBooking(dto.getPatientId(), dto.getPatient()));
        appointment.setDoctor(settingsService.clinicDoctor());
        if (appointment.getAppointmentDate() == null) {
            appointment.setAppointmentDate(LocalDate.now());
        }
        ensureNotAlreadyQueued(appointment);
        applyVisit(appointment, visitFor(appointment.getPatient().getId(), appointment.getAppointmentDate()));
        assignToken(appointment);
        return mapper.toDto(saveAndLog(appointment, "CREATED", "booked for " + Dates.display(appointment.getAppointmentDate())
                + " as " + Enums.label(appointment.getType()).toLowerCase()));
    }

    /** What a booking for this patient (null = new patient) on this date would be: type, fee, validity. */
    @Transactional(readOnly = true)
    public AppointmentDto preview(Long patientId, LocalDate date) {
        Visit visit = visitFor(patientId, date == null ? LocalDate.now() : date);
        AppointmentDto dto = new AppointmentDto();
        dto.setType(visit.type());
        dto.setFee(visit.fee());
        dto.setFollowUpValidUntil(visit.validUntil());
        return dto;
    }

    /** Last day of free follow-up that applies to this visit (shown on the prescription). */
    public LocalDate followUpValidUntil(Appointment appointment) {
        return visitFor(appointment.getPatient().getId(), appointment.getAppointmentDate()).validUntil();
    }

    // ---- status -------------------------------------------------------------------------

    @Transactional
    public AppointmentDto changeStatus(Long id, StatusChangeDto change) {
        Appointment appointment = find(id);
        appointment.setStatus(WorkflowStatus.transition(appointment.getStatus(), AppointmentStatus.class, change.status()));
        return mapper.toDto(saveAndLog(appointment, "STATUS_CHANGED", "marked " + Enums.label(appointment.getStatus()).toLowerCase()));
    }

    /** Marks a waiting appointment completed (when the doctor saves the prescription). */
    public void complete(Appointment appointment) {
        appointment.setStatus(WorkflowStatus.transition(appointment.getStatus(), AppointmentStatus.class, AppointmentStatus.COMPLETED.name()));
        saveAndLog(appointment, "STATUS_CHANGED", "marked completed");
    }

    // ---- report -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ReportDto<AppointmentDto> report(SearchFilter filter) {
        filter.withDefaultPeriod();
        Specification<Appointment> spec = Specs.all(Specs.notDeleted(), filterSpec(filter));
        Map<String, Map<String, Long>> breakdown = new LinkedHashMap<>();
        breakdown.put("type", reportQueries.countBy(Appointment.class, spec, "type"));
        return new ReportDto<>(reportQueries.countWithTotal(Appointment.class, spec, "status"), breakdown,
                reportQueries.countBy(Appointment.class, spec, "appointmentDate"), amounts(spec), page(filter));
    }

    // ---- delete requests ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<DeleteRequestDto> deleteRequests(SearchFilter filter) {
        Specification<AppointmentDeleteRequest> spec = Specs.all(
                Specs.containsAny(filter.getSearch(), "appointmentCode", "patientName", "patientPhone"),
                Specs.eq("status", Enums.parse(DeleteRequestStatus.class, filter.getStatus())),
                Specs.dateTimeBetween("createdAt", filter.getFrom(), filter.getTo()));
        return crud.page(requestRepository, spec, filter.toPageable("createdAt,desc"), mapper::toDto);
    }

    /** Creates a delete request; users allowed to approve deletions remove the booking straight away. */
    @Transactional
    public DeleteRequestDto requestDelete(Long appointmentId, boolean approveNow) {
        Appointment appointment = find(appointmentId);
        ensureDeletable(appointment);
        AppointmentDeleteRequest request = saveAndLog(snapshot(appointment), "CREATED", "Deletion requested");
        return approveNow ? decide(request.getId(), DeleteRequestStatus.APPROVED.name()) : mapper.toDto(request);
    }

    /** Approve (booking permanently removed) or reject a pending request. */
    @Transactional
    public DeleteRequestDto decide(Long requestId, String requestedStatus) {
        AppointmentDeleteRequest request = crud.find(requestRepository, requestId, "Delete request");
        DeleteRequestStatus next = WorkflowStatus.transition(request.getStatus(), DeleteRequestStatus.class, requestedStatus);
        if (next == DeleteRequestStatus.APPROVED) {
            removeAppointment(request);
        }
        request.setStatus(next);
        request.setDecidedBy(CurrentUser.usernameOrSystem());
        request.setDecidedAt(LocalDateTime.now());
        return mapper.toDto(saveAndLog(request, next.name(), "Deletion " + next.name().toLowerCase()));
    }

    // ---- queue helpers ----------------------------------------------------------------------

    private Specification<Appointment> filterSpec(SearchFilter filter) {
        return Specs.all(
                Specs.containsAny(filter.getSearch(), "appointmentCode", "patient.fullName", "patient.phone", "patient.patientCode"),
                Specs.eq("status", Enums.parse(AppointmentStatus.class, filter.getStatus())),
                Specs.eq("type", Enums.parse(AppointmentType.class, filter.getType())),
                Specs.eq("patient.id", filter.getPatientId()),
                Specs.dateBetween("appointmentDate", filter.getFrom(), filter.getTo()),
                withoutPendingDelete());
    }

    /** Bookings with a pending delete request are shown only in the Requests tab, not in the queue or reports. */
    private Specification<Appointment> withoutPendingDelete() {
        return (root, query, cb) -> {
            var sub = query.subquery(Long.class);
            var request = sub.from(AppointmentDeleteRequest.class);
            sub.select(request.get("id")).where(cb.equal(request.get("appointment"), root),
                    cb.equal(request.get("status"), DeleteRequestStatus.PENDING));
            return cb.not(cb.exists(sub));
        };
    }

    private Specification<Appointment> queueOrder() {
        return (root, query, cb) -> {
            if (!Long.class.equals(query.getResultType())) {
                query.orderBy(queueOrders(root, cb));
            }
            return null;
        };
    }

    /** Scheduled before completed; scheduled by date then token; completed newest day first, then token. */
    private List<Order> queueOrders(Root<Appointment> root, CriteriaBuilder cb) {
        var scheduled = cb.equal(root.get("status"), AppointmentStatus.SCHEDULED);
        var waitingFirst = cb.<Integer>selectCase().when(scheduled, 0).otherwise(1);
        var waitingDate = cb.<LocalDate>selectCase().when(scheduled, root.<LocalDate>get("appointmentDate"));
        return List.of(cb.asc(waitingFirst), cb.asc(waitingDate), cb.desc(root.get("appointmentDate")), cb.asc(root.get("tokenNumber")));
    }

    private PageResponse<AppointmentDto> markDeletePending(PageResponse<AppointmentDto> page) {
        List<Long> ids = page.content().stream().map(AppointmentDto::getId).toList();
        if (!ids.isEmpty()) {
            Set<Long> pending = requestRepository.appointmentIdsWithStatus(DeleteRequestStatus.PENDING, ids);
            page.content().forEach(dto -> dto.setDeletePending(pending.contains(dto.getId())));
        }
        return page;
    }

    // ---- booking helpers ----------------------------------------------------------------------

    private void ensureNotAlreadyQueued(Appointment appointment) {
        repository.findFirstByPatientIdAndAppointmentDateAndStatus(appointment.getPatient().getId(),
                        appointment.getAppointmentDate(), AppointmentStatus.SCHEDULED)
                .ifPresent(existing -> {
                    throw BusinessException.conflict(appointment.getPatient().getFullName()
                            + " is already in the queue for this day (token " + existing.getTokenNumber() + ")");
                });
    }

    private void assignToken(Appointment appointment) {
        appointment.setAppointmentCode(codeGenerator.next(CodeType.APPOINTMENT));
        appointment.setTokenNumber(repository.maxToken(appointment.getAppointmentDate()) + 1);
        appointment.setAppointmentTime(LocalTime.now().withNano(0));
        appointment.setStatus(AppointmentStatus.SCHEDULED);
    }

    /**
     * Free follow-up when the date is within the validity days of the last completed consultation
     * (the last valid day counts), otherwise a consultation with the fee from settings.
     */
    private Visit visitFor(Long patientId, LocalDate date) {
        ClinicSettingsDto settings = settingsService.get();
        return repository.findFirstByPatientIdAndStatusAndTypeOrderByAppointmentDateDesc(
                        patientId, AppointmentStatus.COMPLETED, AppointmentType.CONSULTATION)
                .map(last -> last.getAppointmentDate().plusDays(settings.getFollowUpValidityDays()))
                .filter(validUntil -> !date.isAfter(validUntil))
                .map(validUntil -> new Visit(AppointmentType.FOLLOW_UP, BigDecimal.ZERO, validUntil))
                .orElseGet(() -> new Visit(AppointmentType.CONSULTATION, settings.getConsultationFee(),
                        date.plusDays(settings.getFollowUpValidityDays())));
    }

    private void applyVisit(Appointment appointment, Visit visit) {
        appointment.setType(visit.type());
        appointment.setFee(visit.fee());
    }

    /** Informational fee totals only: completed visits and all booked visits. */
    private Map<String, BigDecimal> amounts(Specification<Appointment> spec) {
        Map<String, BigDecimal> amounts = new LinkedHashMap<>();
        amounts.put("completedFees", reportQueries.sum(Appointment.class, Specs.all(spec, Specs.eq("status", AppointmentStatus.COMPLETED)), "fee"));
        amounts.put("expectedFees", reportQueries.sum(Appointment.class, spec, "fee"));
        return amounts;
    }

    // ---- delete request helpers ----------------------------------------------------------------

    private void ensureDeletable(Appointment appointment) {
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw BusinessException.conflict("Only scheduled appointments can be deleted");
        }
        if (requestRepository.existsByAppointmentIdAndStatus(appointment.getId(), DeleteRequestStatus.PENDING)) {
            throw BusinessException.conflict("A delete request for this appointment is already waiting for approval");
        }
    }

    private AppointmentDeleteRequest snapshot(Appointment appointment) {
        AppointmentDeleteRequest request = new AppointmentDeleteRequest();
        request.setAppointment(appointment);
        request.setAppointmentCode(appointment.getAppointmentCode());
        request.setPatientId(appointment.getPatient().getId());
        request.setPatientName(appointment.getPatient().getFullName());
        request.setPatientPhone(appointment.getPatient().getPhone());
        request.setAppointmentDate(appointment.getAppointmentDate());
        request.setTokenNumber(appointment.getTokenNumber());
        return request;
    }

    /** Permanently removes the booking of an approved request. */
    private void removeAppointment(AppointmentDeleteRequest request) {
        Appointment appointment = request.getAppointment();
        if (appointment == null || appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw BusinessException.conflict("This appointment was already completed or removed");
        }
        request.setAppointment(null);
        activityService.log(TYPE, appointment.getId(), appointment.getPatient().getId(), "DELETED",
                label(appointment) + " deleted, approved by " + CurrentUser.usernameOrSystem());
        repository.delete(appointment);
        repository.flush();
    }

    // ---- timeline ---------------------------------------------------------------------------------

    private Appointment saveAndLog(Appointment appointment, String action, String description) {
        Appointment saved = crud.save(repository, appointment);
        activityService.log(TYPE, saved.getId(), saved.getPatient().getId(), action, label(saved) + " " + description);
        return saved;
    }

    private AppointmentDeleteRequest saveAndLog(AppointmentDeleteRequest request, String action, String description) {
        AppointmentDeleteRequest saved = crud.save(requestRepository, request);
        activityService.log(REQUEST_TYPE, saved.getId(), saved.getPatientId(), action, description + " for appointment "
                + saved.getAppointmentCode() + " (token " + saved.getTokenNumber() + ")");
        return saved;
    }

    private String label(Appointment appointment) {
        return "Appointment " + appointment.getAppointmentCode() + " (token " + appointment.getTokenNumber() + ")";
    }

    private record Visit(AppointmentType type, BigDecimal fee, LocalDate validUntil) {
    }
}
