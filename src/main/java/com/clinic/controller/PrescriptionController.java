package com.clinic.controller;

import com.clinic.dto.ApiResponse;
import com.clinic.dto.PrescriptionDto;
import com.clinic.entity.Prescription;
import com.clinic.enums.Access;
import com.clinic.enums.Action;
import com.clinic.enums.Module;
import com.clinic.security.AccessGuard;
import com.clinic.security.ModuleAccess;
import com.clinic.service.PrescriptionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Prescription pad of an appointment. Viewing needs the "view prescriptions" (or write) action; writing needs the
 * "write prescriptions" action; changing a completed prescription is for administrators only.
 */
@RestController
@RequestMapping("/api/appointments/{appointmentId}/prescription")
@ModuleAccess(Module.APPOINTMENT)
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final AccessGuard accessGuard;

    @GetMapping
    public ApiResponse<PrescriptionDto> get(@PathVariable Long appointmentId) {
        accessGuard.requireAny(Action.PRESCRIPTION_VIEW, Action.APPOINTMENT_COMPLETE);
        return ApiResponse.ok(prescriptionService.get(appointmentId, canWrite(), accessGuard.isAdmin()));
    }

    @PutMapping
    public ApiResponse<PrescriptionDto> save(@PathVariable Long appointmentId, @Valid @RequestBody PrescriptionDto dto) {
        accessGuard.requireAction(Action.APPOINTMENT_COMPLETE);
        return ApiResponse.ok(prescriptionService.save(appointmentId, dto, accessGuard.isAdmin()));
    }

    @PostMapping("/complete")
    public ApiResponse<PrescriptionDto> complete(@PathVariable Long appointmentId, @Valid @RequestBody PrescriptionDto dto) {
        accessGuard.requireAction(Action.APPOINTMENT_COMPLETE);
        return ApiResponse.ok("Prescription saved", prescriptionService.complete(appointmentId, dto, accessGuard.isAdmin()));
    }

    private boolean canWrite() {
        return accessGuard.can(Action.APPOINTMENT_COMPLETE) && accessGuard.hasAccess(Module.APPOINTMENT, Access.WRITE);
    }
}
