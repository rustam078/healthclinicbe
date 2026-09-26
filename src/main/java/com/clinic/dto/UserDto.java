package com.clinic.dto;

import com.clinic.dto.ValidationGroups.OnCreate;
import com.clinic.dto.ValidationGroups.OnLogin;
import com.clinic.enums.Access;
import com.clinic.enums.Action;
import com.clinic.enums.Module;
import com.clinic.enums.Role;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.Set;

/** User management payload; also the login request and the "who am I" response (with permissions). */
@Getter
@Setter
public class UserDto extends AuditedDto {

    @NotBlank(groups = {Default.class, OnLogin.class}, message = "Username is required")
    @Pattern(regexp = Patterns.USERNAME, message = "Username must be 3-50 letters, digits, dots, dashes or underscores")
    private String username;

    /** Required on create and login; optional on update (blank keeps the current password). */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(groups = {OnCreate.class, OnLogin.class}, message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be 8-100 characters")
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must be at most 100 characters")
    private String fullName;

    @NotNull(message = "Role is required")
    private Role role;

    private Boolean active;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Map<Module, Access> permissions;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Set<Action> actions;
}
