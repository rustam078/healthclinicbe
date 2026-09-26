package com.clinic.repository;

import com.clinic.entity.Doctor;

import java.util.Optional;

public interface DoctorRepository extends BaseRepository<Doctor> {

    Optional<Doctor> findFirstByActiveTrueAndDeletedFalseOrderByIdAsc();

    long countByDeletedFalse();
}
