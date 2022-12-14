package com.spinel.datacollection.service.repositories;




import com.spinel.datacollection.core.enums.Status;
import com.spinel.datacollection.core.models.EnumeratorProject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EnumeratorProjectRepository extends JpaRepository<EnumeratorProject, Long> {
    EnumeratorProject findByEnumeratorIdAndProjectId(Long enumeratorId, Long projectId);

    List<EnumeratorProject> findByIsActive(Boolean isActive);

    @Query("SELECT e FROM EnumeratorProject e inner  join  Enumerator  j  on e.enumeratorId = j.id WHERE ((:projectId IS NULL) OR (:projectId IS NOT NULL AND e.projectId = :projectId))" +
            " AND ((:enumeratorId IS NULL) OR (:enumeratorId IS NOT NULL AND e.enumeratorId = :enumeratorId)) " +
            " AND ((:verificationStatus IS NULL) OR (:verificationStatus IS NOT NULL AND j.verificationStatus = :verificationStatus)) " +
            " AND ((:assignedDate IS NULL) OR (:assignedDate IS NOT NULL AND e.assignedDate >= :assignedDate)) " +
            " AND ((:completedDate IS NULL) OR (:completedDate IS NOT NULL AND e.completedDate <= :completedDate)) " +
            " AND ((:status IS NULL) OR (:status IS NOT NULL AND e.status = :status)) order by e.id desc")
    Page<EnumeratorProject> findEnumeratorProjects(@Param("projectId") Long projectId,
                                                   @Param("enumeratorId") Long enumeratorId,
                                                   @Param("verificationStatus") String verificationStatus,
                                                   @Param("assignedDate") LocalDateTime assignedDate,
                                                   @Param("completedDate") LocalDateTime completedDate,
                                                   @Param("status") Status status, Pageable pageable);
}

