package com.clinic.mapper;

import com.clinic.dto.PatientDto;
import com.clinic.entity.Patient;
import com.clinic.util.Dates;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = EntityMapperConfig.class, imports = Dates.class)
public interface PatientMapper {

    @Mapping(target = "age", expression = "java(Dates.age(entity.getDateOfBirth()))")
    @Mapping(target = "ageText", expression = "java(Dates.ageText(entity.getDateOfBirth()))")
    PatientDto toDto(Patient entity);

    @Mapping(target = "patientCode", ignore = true)
    Patient toEntity(PatientDto dto);

    @Mapping(target = "patientCode", ignore = true)
    void updateEntity(PatientDto dto, @MappingTarget Patient entity);
}
