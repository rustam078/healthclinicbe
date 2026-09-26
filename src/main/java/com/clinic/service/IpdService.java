package com.clinic.service;

import com.clinic.dto.BedDto;
import com.clinic.dto.IpdAdmissionDto;
import com.clinic.dto.PageResponse;
import com.clinic.dto.ReportDto;
import com.clinic.dto.RoomDto;
import com.clinic.dto.SearchFilter;
import com.clinic.dto.StatusChangeDto;
import com.clinic.entity.Bed;
import com.clinic.entity.IpdAdmission;
import com.clinic.entity.Room;
import com.clinic.enums.BedStatus;
import com.clinic.enums.IpdStatus;
import com.clinic.enums.RoomType;
import com.clinic.exception.BusinessException;
import com.clinic.mapper.IpdMapper;
import com.clinic.repository.BedRepository;
import com.clinic.repository.IpdAdmissionRepository;
import com.clinic.repository.ReportQueries;
import com.clinic.repository.RoomRepository;
import com.clinic.repository.Specs;
import com.clinic.util.CodeGenerator;
import com.clinic.util.CodeGenerator.CodeType;
import com.clinic.util.Enums;
import com.clinic.util.WorkflowStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** IPD module: admissions and discharge, reports, and the rooms and beds they use. */
@Service
@RequiredArgsConstructor
public class IpdService {

    private static final String TYPE = "IPD_ADMISSION";

    private final IpdAdmissionRepository admissionRepository;
    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;
    private final IpdMapper mapper;
    private final CrudSupport crud;
    private final PatientService patientService;
    private final SettingsService settingsService;
    private final ActivityService activityService;
    private final CodeGenerator codeGenerator;
    private final ReportQueries reportQueries;

    // ---- admissions -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<IpdAdmissionDto> page(SearchFilter filter) {
        return crud.page(admissionRepository, admissionSpec(filter), filter.toPageable("admittedAt,desc"), mapper::toDto);
    }

    @Transactional(readOnly = true)
    public IpdAdmissionDto get(Long id) {
        return mapper.toDto(findAdmission(id));
    }

    @Transactional
    public IpdAdmissionDto admit(IpdAdmissionDto dto) {
        if (admissionRepository.existsByPatientIdAndStatusAndDeletedFalse(dto.getPatientId(), IpdStatus.ADMITTED)) {
            throw BusinessException.conflict("This patient is already admitted");
        }
        IpdAdmission admission = mapper.toEntity(dto);
        admission.setPatient(patientService.find(dto.getPatientId()));
        admission.setDoctor(settingsService.clinicDoctor());
        admission.setIpdCode(codeGenerator.next(CodeType.IPD));
        admission.setStatus(IpdStatus.ADMITTED);
        admission.setBed(occupy(dto.getBedId()));
        return mapper.toDto(saveAndLog(admission, "CREATED", "Admitted (" + admission.getIpdCode() + ") to bed " + bedLabel(admission.getBed())));
    }

    /** Edits an active admission; a different bed frees the old one. */
    @Transactional
    public IpdAdmissionDto update(Long id, IpdAdmissionDto dto) {
        IpdAdmission admission = findAdmission(id);
        ensureEditable(admission, dto);
        mapper.updateEntity(dto, admission);
        moveBed(admission, dto.getBedId());
        return mapper.toDto(saveAndLog(admission, "UPDATED", "IPD admission " + admission.getIpdCode() + " updated"));
    }

    @Transactional
    public IpdAdmissionDto discharge(Long id, IpdAdmissionDto dto) {
        IpdAdmission admission = findAdmission(id);
        ensureDischargeable(admission, dto);
        mapper.applyDischarge(dto, admission);
        admission.setStatus(IpdStatus.DISCHARGED);
        admission.getBed().setStatus(BedStatus.AVAILABLE);
        return mapper.toDto(saveAndLog(admission, "DISCHARGED", "Discharged (" + admission.getIpdCode() + ") - "
                + Enums.label(admission.getDischargeCondition())));
    }

    @Transactional(readOnly = true)
    public ReportDto<IpdAdmissionDto> report(SearchFilter filter) {
        filter.withDefaultPeriod();
        Specification<IpdAdmission> spec = Specs.all(Specs.notDeleted(), admissionSpec(filter));
        return new ReportDto<>(reportQueries.countWithTotal(IpdAdmission.class, spec, "status"), breakdown(spec),
                reportQueries.countByDay(IpdAdmission.class, spec, "admittedAt"), amounts(admissionRepository.findAll(spec)), page(filter));
    }

    // ---- rooms ------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<RoomDto> rooms(SearchFilter filter) {
        PageResponse<RoomDto> page = crud.page(roomRepository, roomSpec(filter), filter.toPageable("roomNumber,asc"), mapper::toDto);
        attachOccupants(page.content().stream().map(RoomDto::getBeds).flatMap(Collection::stream).toList());
        return page;
    }

    /** Creates a room and, optionally, its first beds numbered 1..n. */
    @Transactional
    public RoomDto createRoom(RoomDto dto) {
        Room room = mapper.toEntity(dto);
        room.setActive(dto.getActive() == null || dto.getActive());
        Room saved = crud.save(roomRepository, room);
        int count = dto.getInitialBeds() == null ? 0 : dto.getInitialBeds();
        IntStream.rangeClosed(1, count).forEach(number -> addBed(saved, String.valueOf(number)));
        logSettings("ROOM", saved.getId(), "Room " + saved.getRoomNumber() + " created");
        return mapper.toDto(saved);
    }

    @Transactional
    public RoomDto updateRoom(Long id, RoomDto dto) {
        Room room = crud.find(roomRepository, id, "Room");
        mapper.updateEntity(dto, room);
        room.setActive(dto.getActive() == null || dto.getActive());
        logSettings("ROOM", id, "Room " + room.getRoomNumber() + " updated");
        return mapper.toDto(crud.save(roomRepository, room));
    }

    /** Removes a room and its beds (not allowed while a bed is occupied). */
    @Transactional
    public void deleteRoom(Long id) {
        Room room = crud.find(roomRepository, id, "Room");
        if (room.getBeds().stream().anyMatch(bed -> bed.getStatus() == BedStatus.OCCUPIED)) {
            throw BusinessException.conflict("Room " + room.getRoomNumber() + " has occupied beds and cannot be removed");
        }
        room.getBeds().forEach(bed -> bed.setDeleted(true));
        crud.softDelete(roomRepository, room);
        logSettings("ROOM", id, "Room " + room.getRoomNumber() + " removed");
    }

    // ---- beds --------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<BedDto> beds(SearchFilter filter) {
        PageResponse<BedDto> page = crud.page(bedRepository, bedSpec(filter), filter.toPageable("room.roomNumber,asc;bedNumber,asc"), mapper::toDto);
        attachOccupants(page.content());
        return page;
    }

    @Transactional(readOnly = true)
    public BedDto bed(Long id) {
        return mapper.toDto(findBed(id));
    }

    @Transactional
    public BedDto createBed(BedDto dto) {
        Room room = crud.find(roomRepository, dto.getRoomId(), "Room");
        return mapper.toDto(addBed(room, dto.getBedNumber()));
    }

    @Transactional
    public BedDto updateBed(Long id, BedDto dto) {
        Bed bed = findBed(id);
        mapper.updateEntity(dto, bed);
        return mapper.toDto(saveBed(bed, "Bed " + bedLabel(bed) + " renamed"));
    }

    @Transactional
    public void deleteBed(Long id) {
        Bed bed = findBed(id);
        if (bed.getStatus() == BedStatus.OCCUPIED) {
            throw BusinessException.conflict("Bed " + bed.getBedNumber() + " is occupied and cannot be removed");
        }
        bed.setDeleted(true);
        saveBed(bed, "Bed " + bedLabel(bed) + " removed");
    }

    /** Available / reserved / maintenance; OCCUPIED is only set by admission and cleared by discharge. */
    @Transactional
    public BedDto changeBedStatus(Long id, StatusChangeDto change) {
        Bed bed = findBed(id);
        bed.setStatus(WorkflowStatus.transition(bed.getStatus(), BedStatus.class, change.status()));
        return mapper.toDto(saveBed(bed, "Bed " + bedLabel(bed) + " marked " + Enums.label(bed.getStatus()).toLowerCase()));
    }

    // ---- admission helpers ------------------------------------------------------------------------

    private IpdAdmission findAdmission(Long id) {
        return crud.find(admissionRepository, id, "IPD admission");
    }

    private Specification<IpdAdmission> admissionSpec(SearchFilter filter) {
        return Specs.all(
                Specs.containsAny(filter.getSearch(), "ipdCode", "patient.fullName", "patient.phone", "patient.patientCode"),
                Specs.eq("status", Enums.parse(IpdStatus.class, filter.getStatus())),
                Specs.eq("patient.id", filter.getPatientId()),
                Specs.eq("bed.room.id", filter.getRoomId()),
                Specs.eq("bed.room.roomType", Enums.parse(RoomType.class, filter.getType())),
                Specs.dateTimeBetween("admittedAt", filter.getFrom(), filter.getTo()));
    }

    private void ensureEditable(IpdAdmission admission, IpdAdmissionDto dto) {
        if (admission.getStatus() == IpdStatus.DISCHARGED) {
            throw BusinessException.conflict("A discharged admission can no longer be edited");
        }
        if (!admission.getPatient().getId().equals(dto.getPatientId())) {
            throw BusinessException.badRequest("The patient of an admission cannot be changed");
        }
    }

    private void ensureDischargeable(IpdAdmission admission, IpdAdmissionDto dto) {
        if (admission.getStatus() != IpdStatus.ADMITTED) {
            throw BusinessException.conflict("This patient has already been discharged");
        }
        if (dto.getDischargedAt().isBefore(admission.getAdmittedAt())) {
            throw BusinessException.badRequest("Discharge time cannot be before the admission time");
        }
    }

    private void moveBed(IpdAdmission admission, Long bedId) {
        if (!admission.getBed().getId().equals(bedId)) {
            admission.getBed().setStatus(BedStatus.AVAILABLE);
            admission.setBed(occupy(bedId));
        }
    }

    /** Marks a bed occupied for an admission; only available or reserved beds in active rooms can be used. */
    private Bed occupy(Long bedId) {
        Bed bed = findBed(bedId);
        if (!bed.getStatus().isAssignable() || !bed.getRoom().isActive()) {
            throw BusinessException.conflict("Bed " + bedLabel(bed) + " is not available");
        }
        bed.setStatus(BedStatus.OCCUPIED);
        return bed;
    }

    private Map<String, Map<String, Long>> breakdown(Specification<IpdAdmission> spec) {
        Map<String, Map<String, Long>> breakdown = new LinkedHashMap<>();
        breakdown.put("roomType", reportQueries.countBy(IpdAdmission.class, spec, "bed.room.roomType"));
        breakdown.put("dischargeCondition", reportQueries.countBy(IpdAdmission.class,
                Specs.all(spec, Specs.eq("status", IpdStatus.DISCHARGED)), "dischargeCondition"));
        return breakdown;
    }

    /** Estimated charges (informational) and average length of stay for the filtered admissions. */
    private Map<String, BigDecimal> amounts(List<IpdAdmission> admissions) {
        BigDecimal charges = admissions.stream().map(IpdAdmission::estimatedCharges).reduce(BigDecimal.ZERO, BigDecimal::add);
        long days = admissions.stream().mapToLong(IpdAdmission::stayDays).sum();
        BigDecimal average = admissions.isEmpty() ? BigDecimal.ZERO
                : BigDecimal.valueOf(days).divide(BigDecimal.valueOf(admissions.size()), 1, RoundingMode.HALF_UP);
        Map<String, BigDecimal> amounts = new LinkedHashMap<>();
        amounts.put("estimatedCharges", charges);
        amounts.put("averageStayDays", average);
        return amounts;
    }

    private IpdAdmission saveAndLog(IpdAdmission admission, String action, String description) {
        IpdAdmission saved = crud.save(admissionRepository, admission);
        activityService.log(TYPE, saved.getId(), saved.getPatient().getId(), action, description);
        return saved;
    }

    // ---- room & bed helpers ------------------------------------------------------------------------

    private Bed findBed(Long id) {
        return crud.find(bedRepository, id, "Bed");
    }

    private Specification<Room> roomSpec(SearchFilter filter) {
        Boolean active = filter.getStatus() == null ? null : "ACTIVE".equalsIgnoreCase(filter.getStatus());
        return Specs.all(
                Specs.containsAny(filter.getSearch(), "roomNumber", "floor"),
                Specs.eq("roomType", Enums.parse(RoomType.class, filter.getType())),
                Specs.eq("active", active));
    }

    private Specification<Bed> bedSpec(SearchFilter filter) {
        return Specs.all(
                Specs.eq("room.deleted", false),
                Specs.eq("status", Enums.parse(BedStatus.class, filter.getStatus())),
                Specs.eq("room.id", filter.getRoomId()),
                Specs.eq("room.roomType", Enums.parse(RoomType.class, filter.getType())));
    }

    private Bed addBed(Room room, String bedNumber) {
        Bed bed = new Bed();
        bed.setRoom(room);
        bed.setBedNumber(bedNumber);
        room.getBeds().add(bed);
        return saveBed(bed, "Bed " + bedNumber + " added to room " + room.getRoomNumber());
    }

    private Bed saveBed(Bed bed, String description) {
        Bed saved = crud.save(bedRepository, bed);
        logSettings("BED", saved.getId(), description);
        return saved;
    }

    /** Fills current patient details for occupied beds (bed board and room list). */
    private void attachOccupants(List<BedDto> beds) {
        Map<Long, IpdAdmission> byBed = admissionRepository.findByStatusAndDeletedFalse(IpdStatus.ADMITTED).stream()
                .collect(Collectors.toMap(admission -> admission.getBed().getId(), Function.identity()));
        beds.stream().filter(bed -> byBed.containsKey(bed.getId())).forEach(bed -> {
            IpdAdmission admission = byBed.get(bed.getId());
            bed.setCurrentIpdId(admission.getId());
            bed.setCurrentIpdCode(admission.getIpdCode());
            bed.setCurrentPatientName(admission.getPatient().getFullName());
        });
    }

    private void logSettings(String type, Long id, String description) {
        activityService.log(type, id, null, "UPDATED", description);
    }

    private static String bedLabel(Bed bed) {
        return bed.getBedNumber() + " (Room " + bed.getRoom().getRoomNumber() + ")";
    }
}
