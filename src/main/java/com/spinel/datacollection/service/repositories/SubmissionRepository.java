package com.spinel.datacollection.service.repositories;


import com.spinel.datacollection.core.enums.SubmissionStatus;
import com.spinel.datacollection.core.models.Submission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
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
public interface SubmissionRepository extends JpaRepository<Submission, Long>, JpaSpecificationExecutor<Submission> {

    Submission findSubmissionById(Long Id);

    @Query("SELECT s FROM Submission s WHERE ((:isActive IS NULL) OR (:isActive IS NOT NULL AND s.isActive = :isActive))" +
            " AND ((:projectId IS NULL) OR (:projectId IS NOT NULL AND s.projectId = :projectId))" +
            " AND ((:formId IS NULL) OR (:formId IS NOT NULL AND s.formId = :formId))" +
            " AND ((:commentId IS NULL) OR (:commentId IS NOT NULL AND s.commentId = :commentId))" +
            " AND ((:deviceId IS NULL) OR (:deviceId IS NOT NULL AND s.deviceId = :deviceId))" +
            " AND ((:enumeratorId IS NULL) OR (:enumeratorId IS NOT NULL AND s.enumeratorId = :enumeratorId)) order by s.id desc")
    List<Submission> findByIsActive(@Param("isActive") Boolean isActive,
                                    @Param("projectId") Long projectId,
                                    @Param("formId") Long formId,
                                    @Param("commentId") Long commentId,
                                    @Param("deviceId") Long deviceId,
                                    @Param("enumeratorId") Long enumeratorId);


    List<Submission> findAll();

    List<Submission> findSubmissionByProjectId(Long projectId);

    List<Submission> findSubmissionByProjectIdAndStatus(long projectId, SubmissionStatus status);

    List<Submission> findSubmissionBySubmissionDateBetween(LocalDateTime startDate, LocalDateTime endDate);


    @Query("SELECT s FROM Submission s WHERE ((:projectId IS NULL) OR (:projectId IS NOT NULL AND s.projectId = :projectId))" +
            " AND ((:formId IS NULL) OR (:formId IS NOT NULL AND s.formId = :formId))" +
            " AND ((:status IS NULL) OR (:status IS NOT NULL AND s.status = :status))" +
            " AND ((:enumeratorId IS NULL) OR (:enumeratorId IS NOT NULL AND s.enumeratorId = :enumeratorId)) order by s.id desc")
    Page<Submission> findSubmissions(@Param("projectId") Long projectId,
                                     @Param("formId") Long formId,
                                     @Param("status") SubmissionStatus status,
                                     @Param("enumeratorId") Long enumeratorId,
                                     Pageable pageable);

}
