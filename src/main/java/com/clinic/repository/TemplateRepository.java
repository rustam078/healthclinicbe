package com.clinic.repository;

import com.clinic.entity.DocumentTemplate;
import com.clinic.enums.TemplateType;

import java.util.Optional;

public interface TemplateRepository extends BaseRepository<DocumentTemplate> {

    Optional<DocumentTemplate> findByTemplateTypeAndDeletedFalse(TemplateType type);
}
