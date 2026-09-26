package com.clinic.mapper;

import com.clinic.dto.ClinicSettingsDto;
import com.clinic.dto.DoctorDto;
import com.clinic.dto.TemplateDto;
import com.clinic.dto.UserDto;
import com.clinic.entity.AppUser;
import com.clinic.entity.ClinicSettings;
import com.clinic.entity.Doctor;
import com.clinic.entity.DocumentTemplate;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/** Settings module: clinic details, templates, the doctor and user accounts. */
@Mapper(config = EntityMapperConfig.class)
public interface SettingsMapper {

    @Mapping(target = "logoUrl", expression = "java(entity.logoUrl())")
    ClinicSettingsDto toDto(ClinicSettings entity);

    @Mapping(target = "logoPath", ignore = true)
    void updateEntity(ClinicSettingsDto dto, @MappingTarget ClinicSettings entity);

    TemplateDto toDto(DocumentTemplate entity);

    @Mapping(target = "templateType", ignore = true)
    DocumentTemplate toEntity(TemplateDto dto);

    @Mapping(target = "templateType", ignore = true)
    void updateEntity(TemplateDto dto, @MappingTarget DocumentTemplate entity);

    DoctorDto toDto(Doctor entity);

    Doctor toEntity(DoctorDto dto);

    void updateEntity(DoctorDto dto, @MappingTarget Doctor entity);

    @Mapping(target = "password", ignore = true)
    @Mapping(target = "permissions", ignore = true)
    @Mapping(target = "actions", ignore = true)
    UserDto toDto(AppUser entity);

    @Mapping(target = "passwordHash", ignore = true)
    AppUser toEntity(UserDto dto);

    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "username", ignore = true)
    void updateEntity(UserDto dto, @MappingTarget AppUser entity);
}
