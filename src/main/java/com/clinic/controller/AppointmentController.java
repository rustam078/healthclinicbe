package com.clinic.controller;

import com.clinic.dto.ApiResponse;
import com.clinic.dto.AppointmentDto;
import com.clinic.dto.DeleteRequestDto;
import com.clinic.dto.PageResponse;
import com.clinic.dto.ReportDto;
import com.clinic.dto.SearchFilter;
import com.clinic.dto.StatusChangeDto;
import com.clinic.dto.ValidationGroups.OnCreate;
import com.clinic.enums.Action;
import com.clinic.enums.Module;
import com.clinic.security.AccessGuard;
import com.clinic.security.ModuleAccess;
import com.clinic.service.AppointmentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Appointment module: walk-in queue, booking, status, reports and the delete-request workflow. */
@RestController
@RequestMapping("/api")
@ModuleAccess(Module.APPOINTMENT)
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AccessGuard accessGuard;

    @GetMapping("/appointments")
    public ApiResponse<PageResponse<AppointmentDto>> list(SearchFilter filter) {
        return ApiResponse.ok(appointmentService.page(filter));
    }

    @GetMapping("/appointments/{id}")
    public ApiResponse<AppointmentDto> get(@PathVariable Long id) {
        return ApiResponse.ok(appointmentService.get(id));
    }

    @PostMapping("/appointments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AppointmentDto> book(@Validated(OnCreate.class) @RequestBody AppointmentDto dto) {
        return ApiResponse.ok("Appointment booked", appointmentService.book(dto));
    }

    /** Visit type, fee and follow-up validity a booking would get (patientId empty = new patient). */
    @GetMapping("/appointments/preview")
    public ApiResponse<AppointmentDto> preview(@RequestParam(required = false) Long patientId,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(appointmentService.preview(patientId, date));
    }

    @PatchMapping("/appointments/{id}/status")
    public ApiResponse<AppointmentDto> changeStatus(@PathVariable Long id, @Valid @RequestBody StatusChangeDto change) {
        accessGuard.requireAction(Action.APPOINTMENT_COMPLETE);
        return ApiResponse.ok("Appointment status updated", appointmentService.changeStatus(id, change));
    }

    @GetMapping("/appointments/report")
    public ApiResponse<ReportDto<AppointmentDto>> report(SearchFilter filter) {
        return ApiResponse.ok(appointmentService.report(filter));
    }

    /** Users who may approve deletions remove the appointment immediately; others raise a request. */
    @PostMapping("/appointments/{id}/delete-request")
    public ApiResponse<DeleteRequestDto> requestDelete(@PathVariable Long id) {
        boolean approver = accessGuard.can(Action.APPOINTMENT_APPROVE_DELETE);
        String message = approver ? "Appointment deleted" : "Delete request sent for approval";
        return ApiResponse.ok(message, appointmentService.requestDelete(id, approver));
    }

    @GetMapping("/appointment-delete-requests")
    public ApiResponse<PageResponse<DeleteRequestDto>> deleteRequests(SearchFilter filter) {
        return ApiResponse.ok(appointmentService.deleteRequests(filter));
    }

    @PatchMapping("/appointment-delete-requests/{id}/status")
    public ApiResponse<DeleteRequestDto> decide(@PathVariable Long id, @Valid @RequestBody StatusChangeDto change) {
        accessGuard.requireAction(Action.APPOINTMENT_APPROVE_DELETE);
        return ApiResponse.ok("Request updated", appointmentService.decide(id, change.status()));
    }
}
