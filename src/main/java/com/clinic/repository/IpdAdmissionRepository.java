package com.clinic.repository;

import com.clinic.entity.IpdAdmission;
import com.clinic.enums.IpdStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

public interface IpdAdmissionRepository extends BaseRepository<IpdAdmission> {

    @Override
    @EntityGraph(attributePaths = {"patient", "doctor", "bed", "bed.room"})
    Page<IpdAdmission> findAll(Specification<IpdAdmission> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"bed", "bed.room"})
    List<IpdAdmission> findAll(Specification<IpdAdmission> spec);

    @EntityGraph(attributePaths = {"patient", "bed"})
    List<IpdAdmission> findByStatusAndDeletedFalse(IpdStatus status);

    boolean existsByPatientIdAndStatusAndDeletedFalse(Long patientId, IpdStatus status);

    long countByStatusAndDeletedFalse(IpdStatus status);
}
