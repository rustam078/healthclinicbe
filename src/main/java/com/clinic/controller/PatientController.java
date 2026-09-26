package com.clinic.controller;

import com.clinic.dto.ApiResponse;
import com.clinic.dto.PageResponse;
import com.clinic.dto.PatientDto;
import com.clinic.dto.SearchFilter;
import com.clinic.dto.ValidationGroups.OnCreate;
import com.clinic.dto.ValidationGroups.OnUpdate;
import com.clinic.enums.Module;
import com.clinic.security.ModuleAccess;
import com.clinic.service.PatientService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patients")
@ModuleAccess(Module.PATIENT)
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @GetMapping
    public ApiResponse<PageResponse<PatientDto>> list(SearchFilter filter) {
        return ApiResponse.ok(patientService.page(filter));
    }

    @GetMapping("/{id}")
    public ApiResponse<PatientDto> get(@PathVariable Long id) {
        return ApiResponse.ok(patientService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PatientDto> create(@Validated(OnCreate.class) @RequestBody PatientDto dto) {
        return ApiResponse.ok("Patient registered", patientService.create(dto));
    }

    @PutMapping("/{id}")
    public ApiResponse<PatientDto> update(@PathVariable Long id, @Validated(OnUpdate.class) @RequestBody PatientDto dto) {
        return ApiResponse.ok("Patient updated", patientService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        patientService.delete(id);
        return ApiResponse.ok("Patient deleted", null);
    }
}
