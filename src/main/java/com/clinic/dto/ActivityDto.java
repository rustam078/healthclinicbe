package com.clinic.dto;

import com.clinic.entity.ActivityLog;

import java.time.LocalDateTime;

public record ActivityDto(Long id, String entityType, Long entityId, Long patientId, String action,
                          String description, String performedBy, LocalDateTime performedAt) {

    public static ActivityDto of(ActivityLog log) {
        return new ActivityDto(log.getId(), log.getEntityType(), log.getEntityId(), log.getPatientId(),
                log.getAction(), log.getDescription(), log.getPerformedBy(), log.getPerformedAt());
    }
}
