package com.spinel.datacollection.service.repositories;


import com.spinel.datacollection.core.enums.VerificationStatus;
import com.spinel.datacollection.core.models.ProjectEnumerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProjectEnumeratorRepository extends JpaRepository<ProjectEnumerator, Long> {
    ProjectEnumerator findByProjectIdAndEnumeratorId(Long projectId, Long enumeratorId);
    List<ProjectEnumerator> findByProjectId(Long projectId);
    List<ProjectEnumerator> findByEnumeratorId(Long enumeratorId);
    List<ProjectEnumerator> findByEnumeratorIdAndCreatedDateBetween(Long enumeratorId, LocalDateTime start, LocalDateTime end);

    List<ProjectEnumerator> findAllByIsActive(boolean isActive);

//    @Query( "SELECT p FROM ProjectEnumerator p inner join Enumerator  j  on p.enumeratorId = j.id WHERE " +
//        "ProjectEnumerator.enumeratorId=Enumerator.id " +
//        "AND ((:projectId IS NULL) OR (:projectId IS NOT NULL AND p.projectId = :projectId)) " +
//            "AND ((:enumeratorId IS NULL ) OR (:enumeratorId IS NOT NULL AND p.enumeratorId = :enumeratorId)) " +
//            " AND ((:verification IS NULL) OR (:verification IS NOT NULL AND j.verification = :verification)) ")
////        "AND ((:name IS NULL) OR (CONCAT(Enumerator.firstName, \" \" ,Enumerator.lastName) LIKE %:name%) " +
////        "OR (CONCAT(Enumerator.lastName, \" \" ,Enumerator.firstName) LIKE %:name%))", nativeQuery = true)
//    Page<ProjectEnumerator> findProjectEnumerators(Long projectId, Long enumeratorId, VerificationStatus verification, Pageable pageable);

    @Query("SELECT e FROM ProjectEnumerator e inner  join  Enumerator  j  on e.enumeratorId = j.id WHERE ((:projectId IS NULL) OR (:projectId IS NOT NULL AND e.projectId = :projectId))" +
            " AND ((:enumeratorId IS NULL) OR (:enumeratorId IS NOT NULL AND e.enumeratorId = :enumeratorId)) " +
            " AND ((:verification IS NULL) OR (:verification IS NOT NULL AND j.verification = :verification)) " )
//            "AND ((:name IS NULL) OR (CONCAT(Enumerator.firstName, \" \" ,e.lastName) LIKE %:name%) " +
//            "OR (CONCAT(Enumerator.lastName, \" \" ,e.firstName) LIKE %:name%))", nativeQuery = true)
    Page<ProjectEnumerator> findProjectEnumerators(@Param("projectId") Long projectId,
                                                   @Param("enumeratorId") Long enumeratorId,
                                                   @Param("verification") VerificationStatus verification,
                                                    Pageable pageable);


//    @Query("SELECT e FROM ProjectEnumerator e inner  join  Enumerator  j  on e.enumeratorId = j.id WHERE ((:projectId IS NULL) OR (:enumeratorId IS NOT NULL AND e.enumeratorId = :enumeratorId))" +
//            " AND ((:enumeratorId IS NULL) OR (:enumeratorId IS NOT NULL AND e.enumeratorId = :enumeratorId)) " +
//            " AND ((:verification IS NULL) OR (:verification IS NOT NULL AND j.verification = :verification)) " +
//            " AND ((:projectId IS NULL) OR (:projectId IS NOT NULL AND e.projectId >= :projectId)) " +
//            "AND ((:name IS NULL) OR (CONCAT(Enumerator.firstName, \" \" ,e.lastName) LIKE %:name%) " +
//            "OR (CONCAT(Enumerator.lastName, \" \" ,e.firstName) LIKE %:name%))", nativeQuery = true)
//    Page<ProjectEnumerator> findEnumeratorProjects(@Param("projectId") Long projectId,
//                                                   @Param("enumeratorId") Long enumeratorId,
//                                                   @Param("verification") VerificationStatus verification,
//                                                   @Param("projectId") LocalDateTime assignedDate,
//                                                   @Param("completedDate") LocalDateTime completedDate,
//                                                   @Param("status") Status status, Pageable pageable);

    @Query(value = "SELECT ProjectEnumerator.* FROM ProjectEnumerator, Project WHERE " +
            "ProjectEnumerator.projectId=Project.id " +
            "AND ((:enumeratorId IS NULL) OR (:enumeratorId IS NOT NULL AND ProjectEnumerator.enumeratorId = :enumeratorId)) " +
            "AND ((:status IS NULL ) OR (:status IS NOT NULL AND Project.status = :status)) " +
            "AND ((:startDate IS NULL) AND (:endDate IS NULL) OR (ProjectEnumerator.createdDate BETWEEN :startDate AND :endDate))", nativeQuery = true)
    List<ProjectEnumerator> findProjectEnumeratorsWithProjectStatus(Long enumeratorId, String status, LocalDateTime startDate, LocalDateTime endDate);

    @Query(value = "SELECT ProjectEnumerator.* FROM Enumerator, ProjectEnumerator WHERE " +
            "ProjectEnumerator.enumeratorId=Enumerator.id " +
            "AND ((:enumeratorId IS NULL) OR (:enumeratorId IS NOT NULL AND ProjectEnumerator.enumeratorId = :enumeratorId)) " +
            "AND ((:projectId IS NULL ) OR (:projectId IS NOT NULL AND ProjectEnumerator.projectId = :projectId)) " +
            "AND ((:name IS NULL) OR (CONCAT(Enumerator.firstName, \" \" ,Enumerator.lastName) LIKE %:name%) " +
            "OR (CONCAT(Enumerator.lastName, \" \" ,Enumerator.firstName) LIKE %:name%))", nativeQuery = true)
    Page<ProjectEnumerator> findProjectEnumerators(Long projectId,Long enumeratorId,String name,Pageable pageable);
}
