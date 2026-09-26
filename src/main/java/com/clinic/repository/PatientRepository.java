package com.clinic.repository;

import com.clinic.entity.Patient;

public interface PatientRepository extends BaseRepository<Patient> {

    boolean existsByPhoneAndFullNameIgnoreCaseAndDeletedFalseAndIdNot(String phone, String fullName, Long id);
}
