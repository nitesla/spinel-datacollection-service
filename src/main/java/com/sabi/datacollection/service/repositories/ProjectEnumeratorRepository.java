package com.sabi.datacollection.service.repositories;

import com.sabi.datacollection.core.models.ProjectEnumerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectEnumeratorRepository extends JpaRepository<ProjectEnumerator, Long> {
    ProjectEnumerator findByProjectIdAndEnumeratorId(Long projectId, Long enumeratorId);
    List<ProjectEnumerator> findByProjectId(Long projectId);
    List<ProjectEnumerator> findByEnumeratorId(Long enumeratorId);

    List<ProjectEnumerator> findAllByIsActive(boolean isActive);

    @Query(value = "SELECT ProjectEnumerator.* FROM Enumerator, ProjectEnumerator WHERE " +
        "ProjectEnumerator.enumeratorId=Enumerator.id " +
        "AND ((:enumeratorId IS NULL) OR (:enumeratorId IS NOT NULL AND ProjectEnumerator.enumeratorId = :enumeratorId)) " +
        "AND ((:projectId IS NULL ) OR (:projectId IS NOT NULL AND ProjectEnumerator.projectId = :projectId)) " +
        "AND ((:name IS NULL) OR (CONCAT(Enumerator.firstName, \" \" ,Enumerator.lastName) LIKE %:name%) " +
        "OR (CONCAT(Enumerator.lastName, \" \" ,Enumerator.firstName) LIKE %:name%))", nativeQuery = true)
    Page<ProjectEnumerator> findProjectEnumerators(Long projectId,Long enumeratorId,String name,Pageable pageable);
}
