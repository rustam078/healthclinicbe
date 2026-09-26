package com.clinic.controller;

import com.clinic.dto.ActivityDto;
import com.clinic.dto.ApiResponse;
import com.clinic.dto.PageResponse;
import com.clinic.dto.SearchFilter;
import com.clinic.entity.Patient;
import com.clinic.enums.Access;
import com.clinic.enums.Module;
import com.clinic.security.AccessGuard;
import com.clinic.service.ActivityService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Timeline for a patient (patientId) or a single record (entityType + entityId). */
@RestController
@RequestMapping("/api/activity")
@RequiredArgsConstructor
public class ActivityController {

    private static final Map<String, Module> MODULE_BY_TYPE = Map.of(
            "PATIENT", Module.PATIENT,
            "APPOINTMENT", Module.APPOINTMENT,
            "APPOINTMENT_REQUEST", Module.APPOINTMENT,
            "IPD_ADMISSION", Module.IPD);

    private final ActivityService activityService;
    private final AccessGuard accessGuard;

    @GetMapping
    public ApiResponse<PageResponse<ActivityDto>> list(SearchFilter filter) {
        accessGuard.require(moduleFor(filter), Access.READ);
        return ApiResponse.ok(activityService.page(filter));
    }

    /** Patient timelines need Patient access; other record types map to their module; anything else is Settings. */
    private Module moduleFor(SearchFilter filter) {
        if (filter.getEntityType() == null) {
            return filter.getPatientId() != null ? Module.PATIENT : Module.SETTINGS;
        }
        return MODULE_BY_TYPE.getOrDefault(filter.getEntityType().toUpperCase(), Module.SETTINGS);
    }
}
