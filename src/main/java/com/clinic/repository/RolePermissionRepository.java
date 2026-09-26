package com.clinic.repository;

import com.clinic.entity.RolePermission;
import com.clinic.enums.Module;
import com.clinic.enums.Role;

import java.util.Optional;

public interface RolePermissionRepository extends BaseRepository<RolePermission> {

    Optional<RolePermission> findByRoleAndModule(Role role, Module module);
}
