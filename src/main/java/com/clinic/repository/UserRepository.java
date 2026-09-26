package com.clinic.repository;

import com.clinic.entity.AppUser;
import com.clinic.enums.Role;

import java.util.Optional;

public interface UserRepository extends BaseRepository<AppUser> {

    Optional<AppUser> findByUsernameIgnoreCaseAndDeletedFalse(String username);

    long countByRoleAndActiveTrueAndDeletedFalseAndIdNot(Role role, Long excludeId);
}
