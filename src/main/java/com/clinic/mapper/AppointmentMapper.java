package com.clinic.mapper;

import com.clinic.dto.AppointmentDto;
import com.clinic.dto.DeleteRequestDto;
import com.clinic.entity.Appointment;
import com.clinic.entity.AppointmentDeleteRequest;
import com.clinic.util.Dates;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Appointment module: appointments and their delete requests. */
@Mapper(config = EntityMapperConfig.class, imports = Dates.class)
public interface AppointmentMapper {

    @Mapping(target = "patient", ignore = true)
    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "patientName", source = "patient.fullName")
    @Mapping(target = "patientCode", source = "patient.patientCode")
    @Mapping(target = "patientPhone", source = "patient.phone")
    @Mapping(target = "patientGender", source = "patient.gender")
    @Mapping(target = "patientDateOfBirth", source = "patient.dateOfBirth")
    @Mapping(target = "patientAgeText", expression = "java(Dates.ageText(entity.getPatient().getDateOfBirth()))")
    @Mapping(target = "patientAddress", source = "patient.address")
    @Mapping(target = "doctorName", source = "doctor.fullName")
    AppointmentDto toDto(Appointment entity);

    /** Only the date comes from the client; patient, doctor, token, type, fee and status are set by the service. */
    @Mapping(target = "patient", ignore = true)
    @Mapping(target = "doctor", ignore = true)
    @Mapping(target = "appointmentCode", ignore = true)
    @Mapping(target = "tokenNumber", ignore = true)
    @Mapping(target = "appointmentTime", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "fee", ignore = true)
    Appointment toEntity(AppointmentDto dto);

    @Mapping(target = "appointmentId", source = "appointment.id")
    DeleteRequestDto toDto(AppointmentDeleteRequest entity);
}
