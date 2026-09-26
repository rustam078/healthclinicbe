package com.clinic.dto;

import com.clinic.entity.RolePermission;
import com.clinic.enums.Access;
import com.clinic.enums.Module;
import com.clinic.enums.Role;

import jakarta.validation.constraints.NotNull;

public record PermissionDto(@NotNull(message = "Role is required") Role role,
                            @NotNull(message = "Module is required") Module module,
                            @NotNull(message = "Access is required") Access access) {

    public static PermissionDto of(RolePermission permission) {
        return new PermissionDto(permission.getRole(), permission.getModule(), permission.getAccess());
    }
}
