package com.spinel.datacollection.service.repositories;

import com.spinel.datacollection.core.models.JobRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRequestRepository extends JpaRepository<JobRequest, Long> {

    @Query("SELECT o FROM JobRequest o WHERE ((:userId IS NULL) OR (:userId IS NOT NULL AND o.userId = userId)) " +
            "AND ((:projectId IS NULL) OR (:projectId IS NOT NULL AND o.projectId = :projectId)) " +
//            "AND ((:requestedDate IS NULL) OR (:requestedDate IS NOT NULL AND o.requestedDate > :requestedDate)) " +
//            "AND ((:responseDate IS NULL) OR (:responseDate IS NOT NULL AND o.responseDate > :responseDate)) " +
            "AND ((:status IS NULL) OR (:status IS NOT NULL AND o.status = :status))order by o.id desc")
    Page<JobRequest> findJobRequests(@Param("userId") Long userId,
                                     @Param("projectId") Long projectId,
                                     @Param("status") String status,
//                                     @Param("requestedDate") LocalDateTime requestedDate,
//                                     @Param("responseDate") LocalDateTime responseDate,
                                     Pageable pageRequest);

    List<JobRequest> findByIsActive(Boolean isActive);

    JobRequest findByProjectId(Long projectId);
}
