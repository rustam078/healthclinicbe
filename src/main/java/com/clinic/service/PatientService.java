package com.clinic.service;

import com.clinic.dto.PageResponse;
import com.clinic.dto.PatientDto;
import com.clinic.dto.SearchFilter;
import com.clinic.entity.IpdAdmission;
import com.clinic.entity.Patient;
import com.clinic.enums.IpdStatus;
import com.clinic.enums.PatientStatus;
import com.clinic.exception.BusinessException;
import com.clinic.mapper.PatientMapper;
import com.clinic.repository.IpdAdmissionRepository;
import com.clinic.repository.PatientRepository;
import com.clinic.repository.Specs;
import com.clinic.util.CodeGenerator;
import com.clinic.util.CodeGenerator.CodeType;
import com.clinic.util.Enums;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/** Patient module: the central patient record. */
@Service
@RequiredArgsConstructor
public class PatientService {

    private static final String TYPE = "PATIENT";

    private final PatientRepository repository;
    private final IpdAdmissionRepository admissionRepository;
    private final PatientMapper mapper;
    private final CrudSupport crud;
    private final CodeGenerator codeGenerator;
    private final ActivityService activityService;

    @Transactional(readOnly = true)
    public PageResponse<PatientDto> page(SearchFilter filter) {
        return crud.page(repository, filterSpec(filter), filter.toPageable("createdAt,desc"), mapper::toDto);
    }

    @Transactional(readOnly = true)
    public PatientDto get(Long id) {
        return mapper.toDto(find(id));
    }

    @Transactional
    public PatientDto create(PatientDto dto) {
        return mapper.toDto(register(dto));
    }

    @Transactional
    public PatientDto update(Long id, PatientDto dto) {
        Patient patient = find(id);
        mapper.updateEntity(dto, patient);
        prepare(patient);
        return mapper.toDto(saveAndLog(patient, "UPDATED", "Patient details updated"));
    }

    @Transactional
    public void delete(Long id) {
        Patient patient = find(id);
        if (admissionRepository.existsByPatientIdAndStatusAndDeletedFalse(id, IpdStatus.ADMITTED)) {
            throw BusinessException.conflict("This patient is currently admitted and cannot be deleted");
        }
        patient.setDeleted(true);
        saveAndLog(patient, "DELETED", "Patient record deleted");
    }

    public Patient find(Long id) {
        return crud.find(repository, id, "Patient");
    }

    /**
     * Patient for a new appointment: registers a new patient, or uses the selected one and applies
     * corrections made to name, gender, date of birth or address while booking.
     */
    @Transactional
    public Patient forBooking(Long patientId, PatientDto details) {
        if (patientId == null) {
            return register(requireDetails(details));
        }
        Patient patient = find(patientId);
        if (details != null && differs(patient, details)) {
            applyBookingDetails(patient, details);
        }
        return patient;
    }

    // ---- helpers ------------------------------------------------------------------

    private Patient register(PatientDto dto) {
        Patient patient = mapper.toEntity(dto);
        patient.setPatientCode(codeGenerator.next(CodeType.PATIENT));
        prepare(patient);
        return saveAndLog(patient, "CREATED", "Patient registered (" + patient.getPatientCode() + ")");
    }

    private Specification<Patient> filterSpec(SearchFilter filter) {
        return Specs.all(
                Specs.containsAny(filter.getSearch(), "fullName", "phone", "patientCode"),
                Specs.eq("status", Enums.parse(PatientStatus.class, filter.getStatus())),
                Specs.dateTimeBetween("createdAt", filter.getFrom(), filter.getTo()),
                filter.isNotAdmitted() ? notAdmitted() : null);
    }

    private Specification<Patient> notAdmitted() {
        return (root, query, cb) -> {
            var sub = query.subquery(Long.class);
            var admission = sub.from(IpdAdmission.class);
            sub.select(admission.get("id")).where(cb.equal(admission.get("patient"), root),
                    cb.equal(admission.get("status"), IpdStatus.ADMITTED), cb.isFalse(admission.get("deleted")));
            return cb.not(cb.exists(sub));
        };
    }

    private void prepare(Patient patient) {
        if (patient.getStatus() == null) {
            patient.setStatus(PatientStatus.ACTIVE);
        }
        ensureNotDuplicate(patient);
    }

    /** Name + mobile number must be unique (a family may share one number). */
    private void ensureNotDuplicate(Patient patient) {
        Long ownId = patient.getId() == null ? -1L : patient.getId();
        if (repository.existsByPhoneAndFullNameIgnoreCaseAndDeletedFalseAndIdNot(patient.getPhone(), patient.getFullName(), ownId)) {
            throw BusinessException.conflict("A patient named " + patient.getFullName()
                    + " is already registered with this mobile number. Select them from the list instead");
        }
    }

    private PatientDto requireDetails(PatientDto details) {
        if (details == null) {
            throw BusinessException.badRequest("Enter the patient's details or select an existing patient");
        }
        return details;
    }

    private boolean differs(Patient patient, PatientDto details) {
        return !Objects.equals(patient.getFullName(), details.getFullName())
                || patient.getGender() != details.getGender()
                || !Objects.equals(patient.getDateOfBirth(), details.getDateOfBirth())
                || !Objects.equals(patient.getAddress(), details.getAddress());
    }

    private void applyBookingDetails(Patient patient, PatientDto details) {
        patient.setFullName(details.getFullName());
        patient.setGender(details.getGender());
        patient.setDateOfBirth(details.getDateOfBirth());
        patient.setAddress(details.getAddress());
        ensureNotDuplicate(patient);
        saveAndLog(patient, "UPDATED", "Patient details updated while booking an appointment");
    }

    private Patient saveAndLog(Patient patient, String action, String description) {
        Patient saved = crud.save(repository, patient);
        activityService.log(TYPE, saved.getId(), saved.getId(), action, description);
        return saved;
    }
}
