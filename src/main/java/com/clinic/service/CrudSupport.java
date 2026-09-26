package com.clinic.service;

import com.clinic.dto.PageResponse;
import com.clinic.entity.BaseEntity;
import com.clinic.exception.NotFoundException;
import com.clinic.repository.BaseRepository;
import com.clinic.repository.Specs;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.function.Function;

/** Small reusable CRUD steps shared by the module services (find, page, save, soft delete). */
@Component
public class CrudSupport {

    /** Active (not soft-deleted) record or a 404 with a readable name. */
    public <E extends BaseEntity> E find(BaseRepository<E> repository, Long id, String name) {
        return repository.findById(id)
                .filter(entity -> !entity.isDeleted())
                .orElseThrow(() -> new NotFoundException(name, id));
    }

    /** One page of active records matching the filter, converted to DTOs. */
    public <E extends BaseEntity, D> PageResponse<D> page(BaseRepository<E> repository, Specification<E> filter,
                                                          Pageable pageable, Function<E, D> toDto) {
        return PageResponse.of(repository.findAll(Specs.all(Specs.notDeleted(), filter), pageable).map(toDto));
    }

    /** Saves and flushes so audit fields are filled and constraint errors surface immediately. */
    public <E extends BaseEntity> E save(BaseRepository<E> repository, E entity) {
        return repository.saveAndFlush(entity);
    }

    public <E extends BaseEntity> E softDelete(BaseRepository<E> repository, E entity) {
        entity.setDeleted(true);
        return repository.saveAndFlush(entity);
    }
}
