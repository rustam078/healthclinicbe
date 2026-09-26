package com.clinic.service;

import com.clinic.dto.ActivityDto;
import com.clinic.dto.PageResponse;
import com.clinic.dto.SearchFilter;
import com.clinic.entity.ActivityLog;
import com.clinic.repository.ActivityRepository;
import com.clinic.repository.Specs;
import com.clinic.util.CurrentUser;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Writes and reads the timeline shown on patient, appointment and IPD detail screens. */
@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository repository;

    @Transactional
    public void log(String entityType, Long entityId, Long patientId, String action, String description) {
        ActivityLog entry = new ActivityLog();
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setPatientId(patientId);
        entry.setAction(action);
        entry.setDescription(description);
        entry.setPerformedBy(CurrentUser.usernameOrSystem());
        entry.setPerformedAt(LocalDateTime.now());
        repository.save(entry);
    }

    @Transactional(readOnly = true)
    public PageResponse<ActivityDto> page(SearchFilter filter) {
        Specification<ActivityLog> spec = Specs.all(
                Specs.eq("entityType", filter.getEntityType()),
                Specs.eq("entityId", filter.getEntityId()),
                Specs.eq("patientId", filter.getPatientId()));
        return PageResponse.of(repository.findAll(spec, filter.toPageable("performedAt,desc")).map(ActivityDto::of));
    }
}
