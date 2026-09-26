package com.clinic.config;

import com.clinic.dto.AppointmentDto;
import com.clinic.dto.BedDto;
import com.clinic.dto.ClinicSettingsDto;
import com.clinic.dto.DoctorDto;
import com.clinic.dto.IpdAdmissionDto;
import com.clinic.dto.PatientDto;
import com.clinic.dto.RoomDto;
import com.clinic.dto.StatusChangeDto;
import com.clinic.enums.DischargeCondition;
import com.clinic.enums.Gender;
import com.clinic.enums.RoomType;
import com.clinic.repository.PatientRepository;
import com.clinic.service.AppointmentService;
import com.clinic.service.IpdService;
import com.clinic.service.PatientService;
import com.clinic.service.SettingsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Optional sample data for a children's clinic (start with profile "demo" on an empty database).
 * Names are sample data; phone numbers use a clearly fake 90000 range and siblings share a number.
 */
@Slf4j
@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoDataLoader implements ApplicationRunner {

    private static final String[] FIRST = {"Aarav", "Diya", "Kabir", "Ananya", "Vihaan", "Isha", "Rohan", "Meera",
            "Arjun", "Saanvi", "Aditya", "Priya", "Karan", "Neha", "Ishaan", "Pooja", "Rahul", "Kavya", "Siddharth", "Tara"};
    private static final String[] LAST = {"Sharma", "Patel", "Iyer", "Nair", "Gupta", "Reddy", "Das", "Khan", "Joshi", "Mehta"};
    private static final String[] REASONS = {"Fever", "Cough and cold", "Loose motions", "Vaccination review",
            "Rash", "Ear pain", "Feeding concerns", "Growth check"};

    private final SettingsService settingsService;
    private final PatientService patientService;
    private final PatientRepository patientRepository;
    private final AppointmentService appointmentService;
    private final IpdService ipdService;
    private final Random random = new Random(42);

    @Override
    public void run(ApplicationArguments args) {
        if (alreadyHasData()) {
            return;
        }
        setUpDoctorAndFees();
        List<Long> beds = createRooms();
        List<Long> patients = IntStream.range(0, 40).mapToObj(this::createPatient).toList();
        createAppointments(patients);
        createAdmissions(patients, beds);
        log.info("Demo data created");
    }

    private boolean alreadyHasData() {
        boolean hasData = patientRepository.count() > 5;
        if (hasData) {
            log.info("Demo data skipped: database already has patients");
        }
        return hasData;
    }

    private void setUpDoctorAndFees() {
        DoctorDto doctor = new DoctorDto();
        doctor.setFullName("Dr. Meera Iyer");
        doctor.setSpecialization("MBBS, MD (Paediatrics)");
        settingsService.createDoctor(doctor);
        ClinicSettingsDto settings = settingsService.get();
        settings.setClinicName("Little Steps Children's Clinic (Sample)");
        settings.setConsultationFee(BigDecimal.valueOf(500));
        settings.setFollowUpValidityDays(30);
        settingsService.update(settings);
    }

    private List<Long> createRooms() {
        return Stream.of(room("101", RoomType.GENERAL, 1200, 6), room("102", RoomType.SEMI_PRIVATE, 2200, 2),
                        room("201", RoomType.PRIVATE, 3500, 1), room("NICU-1", RoomType.ICU, 6000, 2))
                .flatMap(room -> room.getBeds().stream().map(BedDto::getId)).toList();
    }

    private RoomDto room(String number, RoomType type, int charge, int beds) {
        RoomDto dto = new RoomDto();
        dto.setRoomNumber(number);
        dto.setRoomType(type);
        dto.setDailyCharge(BigDecimal.valueOf(charge));
        dto.setFloor(number.startsWith("2") ? "2" : "1");
        dto.setInitialBeds(beds);
        return ipdService.createRoom(dto);
    }

    /** Children aged a few days to 12 years; every pair of children shares a parent's mobile number. */
    private Long createPatient(int index) {
        PatientDto dto = new PatientDto();
        dto.setFullName(FIRST[index % FIRST.length] + " " + LAST[(index / 2) % LAST.length]);
        dto.setPhone(String.format("90000%05d", index / 2 + 1));
        dto.setGender(index % 2 == 0 ? Gender.MALE : Gender.FEMALE);
        dto.setDateOfBirth(LocalDate.now().minusDays(10 + random.nextInt(365 * 12)));
        dto.setAddress("Sample address " + (index / 2 + 1));
        return patientService.create(dto).getId();
    }

    /** Past days are fully served; today's queue is partly served; a few bookings for the coming days. */
    private void createAppointments(List<Long> patients) {
        for (int day = -45; day <= 3; day++) {
            int perDay = day > 0 ? 2 : 4 + random.nextInt(5);
            for (int slot = 0; slot < perDay; slot++) {
                book(patients.get(random.nextInt(patients.size())), LocalDate.now().plusDays(day), slot);
            }
        }
    }

    private void book(Long patientId, LocalDate date, int slot) {
        AppointmentDto dto = new AppointmentDto();
        dto.setPatientId(patientId);
        dto.setAppointmentDate(date);
        try {
            AppointmentDto booked = appointmentService.book(dto);
            settle(booked, date, slot);
        } catch (RuntimeException alreadyQueued) {
            log.debug("Skipped duplicate demo booking: {}", alreadyQueued.getMessage());
        }
    }

    private void settle(AppointmentDto booked, LocalDate date, int slot) {
        boolean served = date.isBefore(LocalDate.now()) || (date.isEqual(LocalDate.now()) && slot < 2);
        if (served) {
            appointmentService.changeStatus(booked.getId(), new StatusChangeDto("COMPLETED", null));
        } else if (date.isAfter(LocalDate.now()) && slot == 1) {
            appointmentService.requestDelete(booked.getId(), false);
        }
    }

    private void createAdmissions(List<Long> patients, List<Long> beds) {
        for (int i = 0; i < 7; i++) {
            int daysAgo = i < 3 ? 6 + random.nextInt(14) : 1 + random.nextInt(6);
            IpdAdmissionDto admitted = admit(patients.get(i), beds.get(i), daysAgo);
            if (i < 3) {
                discharge(admitted);
            }
        }
    }

    private IpdAdmissionDto admit(Long patientId, Long bedId, int daysAgo) {
        IpdAdmissionDto dto = new IpdAdmissionDto();
        dto.setPatientId(patientId);
        dto.setBedId(bedId);
        dto.setAdmittedAt(LocalDate.now().minusDays(daysAgo).atTime(10, 0));
        dto.setExpectedDischargeDate(LocalDate.now().plusDays(random.nextInt(4)));
        dto.setReason(REASONS[random.nextInt(REASONS.length)] + " - observation");
        return ipdService.admit(dto);
    }

    private void discharge(IpdAdmissionDto admission) {
        IpdAdmissionDto dto = new IpdAdmissionDto();
        dto.setDischargedAt(admission.getAdmittedAt().plusDays(1 + random.nextInt(3)).withHour(11));
        dto.setDischargeCondition(DischargeCondition.RECOVERED);
        dto.setDischargeSummary("Treated and stable at discharge. (Sample record)");
        ipdService.discharge(admission.getId(), dto);
    }
}
