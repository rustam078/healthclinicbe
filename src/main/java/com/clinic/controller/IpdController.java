package com.clinic.controller;

import com.clinic.dto.ApiResponse;
import com.clinic.dto.BedDto;
import com.clinic.dto.IpdAdmissionDto;
import com.clinic.dto.PageResponse;
import com.clinic.dto.ReportDto;
import com.clinic.dto.RoomDto;
import com.clinic.dto.SearchFilter;
import com.clinic.dto.StatusChangeDto;
import com.clinic.dto.ValidationGroups.OnCreate;
import com.clinic.dto.ValidationGroups.OnDischarge;
import com.clinic.dto.ValidationGroups.OnUpdate;
import com.clinic.enums.Action;
import com.clinic.enums.Module;
import com.clinic.security.AccessGuard;
import com.clinic.security.ModuleAccess;
import com.clinic.service.IpdService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * IPD module: admissions, discharge and reports, plus beds (IPD access, readable by everyone)
 * and rooms (managed from Settings, readable by everyone).
 */
@RestController
@RequestMapping("/api")
@ModuleAccess(Module.IPD)
@RequiredArgsConstructor
public class IpdController {

    private final IpdService ipdService;
    private final AccessGuard accessGuard;

    // ---- admissions -------------------------------------------------------------------------

    @GetMapping("/ipd")
    public ApiResponse<PageResponse<IpdAdmissionDto>> list(SearchFilter filter) {
        return ApiResponse.ok(ipdService.page(filter));
    }

    @GetMapping("/ipd/{id}")
    public ApiResponse<IpdAdmissionDto> get(@PathVariable Long id) {
        return ApiResponse.ok(ipdService.get(id));
    }

    @PostMapping("/ipd")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<IpdAdmissionDto> admit(@Validated(OnCreate.class) @RequestBody IpdAdmissionDto dto) {
        return ApiResponse.ok("Patient admitted", ipdService.admit(dto));
    }

    @PutMapping("/ipd/{id}")
    public ApiResponse<IpdAdmissionDto> update(@PathVariable Long id, @Validated(OnUpdate.class) @RequestBody IpdAdmissionDto dto) {
        return ApiResponse.ok("Admission updated", ipdService.update(id, dto));
    }

    @PostMapping("/ipd/{id}/discharge")
    public ApiResponse<IpdAdmissionDto> discharge(@PathVariable Long id, @Validated(OnDischarge.class) @RequestBody IpdAdmissionDto dto) {
        accessGuard.requireAction(Action.IPD_DISCHARGE);
        return ApiResponse.ok("Patient discharged", ipdService.discharge(id, dto));
    }

    @GetMapping("/ipd/report")
    public ApiResponse<ReportDto<IpdAdmissionDto>> report(SearchFilter filter) {
        return ApiResponse.ok(ipdService.report(filter));
    }

    // ---- rooms (Settings) -------------------------------------------------------------------

    @GetMapping("/rooms")
    @ModuleAccess(value = Module.SETTINGS, readOpen = true)
    public ApiResponse<PageResponse<RoomDto>> rooms(SearchFilter filter) {
        return ApiResponse.ok(ipdService.rooms(filter));
    }

    @PostMapping("/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    @ModuleAccess(Module.SETTINGS)
    public ApiResponse<RoomDto> createRoom(@Validated(OnCreate.class) @RequestBody RoomDto dto) {
        return ApiResponse.ok("Room saved", ipdService.createRoom(dto));
    }

    @PutMapping("/rooms/{id}")
    @ModuleAccess(Module.SETTINGS)
    public ApiResponse<RoomDto> updateRoom(@PathVariable Long id, @Validated(OnUpdate.class) @RequestBody RoomDto dto) {
        return ApiResponse.ok("Room updated", ipdService.updateRoom(id, dto));
    }

    @DeleteMapping("/rooms/{id}")
    @ModuleAccess(Module.SETTINGS)
    public ApiResponse<Void> deleteRoom(@PathVariable Long id) {
        ipdService.deleteRoom(id);
        return ApiResponse.ok("Room deleted", null);
    }

    // ---- beds -------------------------------------------------------------------------------

    @GetMapping("/beds")
    @ModuleAccess(value = Module.IPD, readOpen = true)
    public ApiResponse<PageResponse<BedDto>> beds(SearchFilter filter) {
        return ApiResponse.ok(ipdService.beds(filter));
    }

    @GetMapping("/beds/{id}")
    @ModuleAccess(value = Module.IPD, readOpen = true)
    public ApiResponse<BedDto> bed(@PathVariable Long id) {
        return ApiResponse.ok(ipdService.bed(id));
    }

    @PostMapping("/beds")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BedDto> createBed(@Validated(OnCreate.class) @RequestBody BedDto dto) {
        return ApiResponse.ok("Bed saved", ipdService.createBed(dto));
    }

    @PutMapping("/beds/{id}")
    public ApiResponse<BedDto> updateBed(@PathVariable Long id, @Validated(OnUpdate.class) @RequestBody BedDto dto) {
        return ApiResponse.ok("Bed updated", ipdService.updateBed(id, dto));
    }

    @DeleteMapping("/beds/{id}")
    public ApiResponse<Void> deleteBed(@PathVariable Long id) {
        ipdService.deleteBed(id);
        return ApiResponse.ok("Bed deleted", null);
    }

    @PatchMapping("/beds/{id}/status")
    public ApiResponse<BedDto> changeBedStatus(@PathVariable Long id, @Valid @RequestBody StatusChangeDto change) {
        return ApiResponse.ok("Bed status updated", ipdService.changeBedStatus(id, change));
    }
}
