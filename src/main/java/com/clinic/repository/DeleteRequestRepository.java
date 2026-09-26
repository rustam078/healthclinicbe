package com.clinic.repository;

import com.clinic.entity.AppointmentDeleteRequest;
import com.clinic.enums.DeleteRequestStatus;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Set;

public interface DeleteRequestRepository extends BaseRepository<AppointmentDeleteRequest> {

    boolean existsByAppointmentIdAndStatus(Long appointmentId, DeleteRequestStatus status);

    long countByStatusAndDeletedFalse(DeleteRequestStatus status);

    @Query("select r.appointment.id from AppointmentDeleteRequest r where r.status = :status and r.appointment.id in :ids")
    Set<Long> appointmentIdsWithStatus(@Param("status") DeleteRequestStatus status, @Param("ids") Collection<Long> ids);
}
