package com.clinic.entity;

import com.clinic.enums.Access;
import com.clinic.enums.Module;
import com.clinic.enums.Role;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "role_permissions")
public class RolePermission extends BaseEntity {

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private Module module;

    @Enumerated(EnumType.STRING)
    private Access access;
}
