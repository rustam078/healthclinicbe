package com.clinic.repository;

import com.clinic.entity.Bed;
import com.clinic.enums.BedStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;

public interface BedRepository extends BaseRepository<Bed> {

    @Override
    @EntityGraph(attributePaths = "room")
    Page<Bed> findAll(Specification<Bed> spec, Pageable pageable);

    long countByStatusAndDeletedFalseAndRoomDeletedFalse(BedStatus status);

    long countByDeletedFalseAndRoomDeletedFalse();
}
