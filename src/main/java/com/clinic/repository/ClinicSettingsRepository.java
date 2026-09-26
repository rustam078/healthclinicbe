package com.clinic.repository;

import com.clinic.entity.ClinicSettings;

import java.util.Optional;

public interface ClinicSettingsRepository extends BaseRepository<ClinicSettings> {

    Optional<ClinicSettings> findFirstByDeletedFalseOrderByIdAsc();
}
