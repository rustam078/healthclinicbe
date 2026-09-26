package com.clinic.dto;

import com.clinic.enums.DeleteRequestStatus;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Read-only view of a delete request (requested by / at come from the audit fields createdBy / createdAt). */
@Getter
@Setter
public class DeleteRequestDto extends AuditedDto {

    private Long appointmentId;
    private String appointmentCode;
    private Long patientId;
    private String patientName;
    private String patientPhone;
    private LocalDate appointmentDate;
    private Integer tokenNumber;
    private DeleteRequestStatus status;
    private String decidedBy;
    private LocalDateTime decidedAt;
}
