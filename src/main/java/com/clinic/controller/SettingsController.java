package com.clinic.controller;

import com.clinic.dto.ApiResponse;
import com.clinic.dto.ClinicSettingsDto;
import com.clinic.dto.DoctorDto;
import com.clinic.dto.PageResponse;
import com.clinic.dto.PermissionDto;
import com.clinic.dto.RoleActionDto;
import com.clinic.dto.SearchFilter;
import com.clinic.dto.TemplateDto;
import com.clinic.dto.UserDto;
import com.clinic.dto.ValidationGroups.OnCreate;
import com.clinic.dto.ValidationGroups.OnUpdate;
import com.clinic.enums.Module;
import com.clinic.security.ModuleAccess;
import com.clinic.service.AuthService;
import com.clinic.service.SettingsService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Settings module. Clinic details, templates and the doctor are readable by every signed-in user
 * (printing and booking need them); changes need Settings write access. Users and permissions are admin only.
 */
@RestController
@RequestMapping("/api")
@ModuleAccess(value = Module.SETTINGS, readOpen = true)
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;
    private final AuthService authService;

    // ---- clinic details & logo ----------------------------------------------------------------

    @GetMapping("/settings")
    public ApiResponse<ClinicSettingsDto> get() {
        return ApiResponse.ok(settingsService.get());
    }

    @PutMapping("/settings")
    public ApiResponse<ClinicSettingsDto> update(@Valid @RequestBody ClinicSettingsDto dto) {
        return ApiResponse.ok("Clinic settings saved", settingsService.update(dto));
    }

    @PostMapping(value = "/settings/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ClinicSettingsDto> uploadLogo(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok("Logo saved", settingsService.uploadLogo(file));
    }

    @DeleteMapping("/settings/logo")
    public ApiResponse<ClinicSettingsDto> removeLogo() {
        return ApiResponse.ok("Logo removed", settingsService.removeLogo());
    }

    @GetMapping("/settings/logo")
    public ResponseEntity<Resource> logo() {
        SettingsService.LogoFile logo = settingsService.loadLogo();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(logo.contentType()))
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .header("X-Content-Type-Options", "nosniff")
                .body(logo.resource());
    }

    // ---- templates ----------------------------------------------------------------------------

    @GetMapping("/templates")
    public ApiResponse<List<TemplateDto>> templates() {
        return ApiResponse.ok(settingsService.templates());
    }

    @GetMapping("/templates/{type}")
    public ApiResponse<TemplateDto> template(@PathVariable String type) {
        return ApiResponse.ok(settingsService.template(type));
    }

    @PutMapping("/templates/{type}")
    public ApiResponse<TemplateDto> updateTemplate(@PathVariable String type, @Valid @RequestBody TemplateDto dto) {
        return ApiResponse.ok("Template saved", settingsService.updateTemplate(type, dto));
    }

    @PostMapping("/templates/{type}/reset")
    public ApiResponse<TemplateDto> resetTemplate(@PathVariable String type) {
        return ApiResponse.ok("Template reset to default", settingsService.resetTemplate(type));
    }

    // ---- doctor -------------------------------------------------------------------------------

    @GetMapping("/doctors")
    public ApiResponse<PageResponse<DoctorDto>> doctors(SearchFilter filter) {
        return ApiResponse.ok(settingsService.doctors(filter));
    }

    /** The clinic's doctor (null when not set up yet). */
    @GetMapping("/doctors/clinic")
    public ApiResponse<DoctorDto> clinicDoctor() {
        return ApiResponse.ok(settingsService.clinicDoctorOrNull());
    }

    @PostMapping("/doctors")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DoctorDto> createDoctor(@Validated(OnCreate.class) @RequestBody DoctorDto dto) {
        return ApiResponse.ok("Doctor saved", settingsService.createDoctor(dto));
    }

    @PutMapping("/doctors/{id}")
    public ApiResponse<DoctorDto> updateDoctor(@PathVariable Long id, @Validated(OnUpdate.class) @RequestBody DoctorDto dto) {
        return ApiResponse.ok("Doctor updated", settingsService.updateDoctor(id, dto));
    }

    // ---- users (admin only) -------------------------------------------------------------------

    @GetMapping("/users")
    @ModuleAccess(value = Module.SETTINGS, adminOnly = true)
    public ApiResponse<PageResponse<UserDto>> users(SearchFilter filter) {
        return ApiResponse.ok(authService.users(filter));
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    @ModuleAccess(value = Module.SETTINGS, adminOnly = true)
    public ApiResponse<UserDto> createUser(@Validated(OnCreate.class) @RequestBody UserDto dto) {
        return ApiResponse.ok("User saved", authService.createUser(dto));
    }

    @PutMapping("/users/{id}")
    @ModuleAccess(value = Module.SETTINGS, adminOnly = true)
    public ApiResponse<UserDto> updateUser(@PathVariable Long id, @Validated(OnUpdate.class) @RequestBody UserDto dto) {
        return ApiResponse.ok("User updated", authService.updateUser(id, dto));
    }

    @DeleteMapping("/users/{id}")
    @ModuleAccess(value = Module.SETTINGS, adminOnly = true)
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        authService.deleteUser(id);
        return ApiResponse.ok("User deleted", null);
    }

    // ---- permissions (admin only) -------------------------------------------------------------

    @GetMapping("/permissions")
    @ModuleAccess(value = Module.SETTINGS, adminOnly = true)
    public ApiResponse<List<PermissionDto>> permissions() {
        return ApiResponse.ok(authService.permissions());
    }

    @PutMapping("/permissions")
    @ModuleAccess(value = Module.SETTINGS, adminOnly = true)
    public ApiResponse<List<PermissionDto>> updatePermissions(@RequestBody List<@Valid PermissionDto> changes) {
        return ApiResponse.ok("Permissions saved", authService.updatePermissions(changes));
    }

    @GetMapping("/permissions/actions")
    @ModuleAccess(value = Module.SETTINGS, adminOnly = true)
    public ApiResponse<List<RoleActionDto>> actions() {
        return ApiResponse.ok(authService.actions());
    }

    @PutMapping("/permissions/actions")
    @ModuleAccess(value = Module.SETTINGS, adminOnly = true)
    public ApiResponse<List<RoleActionDto>> updateActions(@RequestBody List<@Valid RoleActionDto> changes) {
        return ApiResponse.ok("Action permissions saved", authService.updateActions(changes));
    }
}
