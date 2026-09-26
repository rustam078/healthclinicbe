package com.clinic.repository;

import com.clinic.entity.RoleAction;
import com.clinic.enums.Action;
import com.clinic.enums.Role;

import java.util.Optional;

public interface RoleActionRepository extends BaseRepository<RoleAction> {

    Optional<RoleAction> findByRoleAndAction(Role role, Action action);
}
