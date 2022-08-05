package com.sabi.datacollection.service.repositories;

import com.sabi.datacollection.core.enums.Status;
import com.sabi.datacollection.core.models.Submission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 *
 * This interface is responsible for Submission crud operations
 */

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    Submission findSubmissionById(Long Id);

    List<Submission> findByIsActive(Boolean isActive);

    List<Submission> findAll();

    List<Submission> findSubmissionByProjectId(Long projectId);

    List<Submission> findSubmissionByProjectIdAndStatus(long projectId, Status status);

    List<Submission> findSubmissionBySubmissionDateBetween(LocalDateTime startDate, LocalDateTime endDate);


    @Query("SELECT s FROM Submission s WHERE ((:projectId IS NULL) OR (:projectId IS NOT NULL AND s.projectId = :projectId))" +
            " AND ((:formId IS NULL) OR (:formId IS NOT NULL AND s.formId = :formId))" +
            " AND ((:status IS NULL) OR (:status IS NOT NULL AND s.status = :status))" +
            " AND ((:enumeratorId IS NULL) OR (:enumeratorId IS NOT NULL AND s.enumeratorId = :enumeratorId)) order by s.id desc")
    Page<Submission> findSubmissions(@Param("projectId") Long projectId,
                                     @Param("formId") Long formId,
                                     @Param("status") Status status,
                                     @Param("enumeratorId") Long enumeratorId,
                                     Pageable pageable);

}
