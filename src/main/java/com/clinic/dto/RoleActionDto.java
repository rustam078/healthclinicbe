package com.clinic.dto;

import com.clinic.entity.RoleAction;
import com.clinic.enums.Action;
import com.clinic.enums.Role;

import jakarta.validation.constraints.NotNull;

public record RoleActionDto(@NotNull(message = "Role is required") Role role,
                            @NotNull(message = "Action is required") Action action,
                            boolean allowed) {

    public static RoleActionDto of(RoleAction entity) {
        return new RoleActionDto(entity.getRole(), entity.getAction(), entity.isAllowed());
    }
}
