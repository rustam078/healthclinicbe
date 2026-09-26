package com.clinic.controller;

import com.clinic.dto.ApiResponse;
import com.clinic.dto.DashboardDto;
import com.clinic.enums.Module;
import com.clinic.exception.BusinessException;
import com.clinic.security.ModuleAccess;
import com.clinic.service.DashboardService;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
@ModuleAccess(Module.DASHBOARD)
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ApiResponse<DashboardDto> overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (from != null && to != null && to.isBefore(from)) {
            throw BusinessException.badRequest("The end date cannot be before the start date");
        }
        return ApiResponse.ok(dashboardService.overview(from, to));
    }
}
