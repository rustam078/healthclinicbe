package com.clinic.mapper;

import com.clinic.dto.BedDto;
import com.clinic.dto.IpdAdmissionDto;
import com.clinic.dto.RoomDto;
import com.clinic.entity.Bed;
import com.clinic.entity.IpdAdmission;
import com.clinic.entity.Room;
import com.clinic.util.Dates;

import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/** IPD module: admissions, rooms and beds. */
@Mapper(config = EntityMapperConfig.class, imports = Dates.class)
public interface IpdMapper {

    // ---- admissions ---------------------------------------------------------------

    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "patientName", source = "patient.fullName")
    @Mapping(target = "patientCode", source = "patient.patientCode")
    @Mapping(target = "patientPhone", source = "patient.phone")
    @Mapping(target = "patientGender", source = "patient.gender")
    @Mapping(target = "patientAge", expression = "java(Dates.age(entity.getPatient().getDateOfBirth()))")
    @Mapping(target = "patientAgeText", expression = "java(Dates.ageText(entity.getPatient().getDateOfBirth()))")
    @Mapping(target = "doctorId", source = "doctor.id")
    @Mapping(target = "doctorName", source = "doctor.fullName")
    @Mapping(target = "bedId", source = "bed.id")
    @Mapping(target = "bedNumber", source = "bed.bedNumber")
    @Mapping(target = "roomNumber", source = "bed.room.roomNumber")
    @Mapping(target = "roomType", source = "bed.room.roomType")
    @Mapping(target = "dailyCharge", source = "bed.room.dailyCharge")
    IpdAdmissionDto toDto(IpdAdmission entity);

    @Mapping(target = "patient", ignore = true)
    @Mapping(target = "doctor", ignore = true)
    @Mapping(target = "bed", ignore = true)
    @Mapping(target = "ipdCode", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "dischargedAt", ignore = true)
    @Mapping(target = "dischargeCondition", ignore = true)
    @Mapping(target = "dischargeNotes", ignore = true)
    @Mapping(target = "dischargeSummary", ignore = true)
    IpdAdmission toEntity(IpdAdmissionDto dto);

    @Mapping(target = "patient", ignore = true)
    @Mapping(target = "doctor", ignore = true)
    @Mapping(target = "bed", ignore = true)
    @Mapping(target = "ipdCode", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "dischargedAt", ignore = true)
    @Mapping(target = "dischargeCondition", ignore = true)
    @Mapping(target = "dischargeNotes", ignore = true)
    @Mapping(target = "dischargeSummary", ignore = true)
    void updateEntity(IpdAdmissionDto dto, @MappingTarget IpdAdmission entity);

    /** Copies only the discharge fields. */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "dischargedAt", source = "dischargedAt")
    @Mapping(target = "dischargeCondition", source = "dischargeCondition")
    @Mapping(target = "dischargeNotes", source = "dischargeNotes")
    @Mapping(target = "dischargeSummary", source = "dischargeSummary")
    void applyDischarge(IpdAdmissionDto dto, @MappingTarget IpdAdmission entity);

    @AfterMapping
    default void fillStay(IpdAdmission entity, @MappingTarget IpdAdmissionDto dto) {
        dto.setStayDays(entity.stayDays());
        dto.setEstimatedCharges(entity.estimatedCharges());
    }

    // ---- rooms & beds -------------------------------------------------------------

    @Mapping(target = "initialBeds", ignore = true)
    RoomDto toDto(Room entity);

    @Mapping(target = "beds", ignore = true)
    Room toEntity(RoomDto dto);

    @Mapping(target = "beds", ignore = true)
    void updateEntity(RoomDto dto, @MappingTarget Room entity);

    @Mapping(target = "roomId", source = "room.id")
    @Mapping(target = "roomNumber", source = "room.roomNumber")
    @Mapping(target = "roomType", source = "room.roomType")
    @Mapping(target = "dailyCharge", source = "room.dailyCharge")
    BedDto toDto(Bed entity);

    @Mapping(target = "room", ignore = true)
    @Mapping(target = "status", ignore = true)
    Bed toEntity(BedDto dto);

    @Mapping(target = "room", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updateEntity(BedDto dto, @MappingTarget Bed entity);
}
