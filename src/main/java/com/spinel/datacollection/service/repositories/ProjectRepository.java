package com.spinel.datacollection.service.repositories;


import com.spinel.datacollection.core.enums.Status;
import com.spinel.datacollection.core.models.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {
    Project findByName(String name);

    List<Project> findByStatus(Status status);

//    List<Project> findByStatusAndProjectCategoryId(Status status, Long categoryId);

//    List<Project> findByProjectCategoryId(Long categoryId);

    Page<Project> findByProjectOwnerId(Long projectOwnerId, Pageable pageable);

    Page<Project> findByProjectOwnerIdAndStatus(Long projectOwnerId, Status status, Pageable pageable);

    List<Project> findByProjectOwnerId(Long projectOwnerId);

    List<Project> findByProjectOwnerIdAndStatus(Long projectOwnerId, Status status);

    List<Project> findByProjectOwnerIdAndCreatedDateBetween(Long projectOwnerId, LocalDateTime start, LocalDateTime end);

    List<Project> findByProjectOwnerIdAndStatusAndCreatedDateBetween(Long projectOwnerId, Status status, LocalDateTime start, LocalDateTime end);

    List<Project> findByIsActive(Boolean isActive);

//    @Query(value = "SELECT Project.* from Project, ProjectCategory WHERE " +
//            "Project.projectCategoryId=ProjectCategory.id " +
//            "AND ((:name IS NULL) OR (:name IS NOT NULL AND Project.name like %:name%)) " +
//            "AND ((:status IS NULL) OR (:status IS NOT NULL AND Project.status = :status)) " +
//            "AND ((:category IS NULL) OR (:category IS NOT NULL AND ProjectCategory .name LIKE %:category%))", nativeQuery = true)
//    Page<Project> findProjects(@Param("name") String name, String status, String category,
//                               Pageable pageable);

    @Query(value = "SELECT * from Project WHERE  " +
            "((:name IS NULL) OR (:name IS NOT NULL AND Project.name like %:name%)) " +
            "AND ((:status IS NULL) OR (:status IS NOT NULL AND Project.status = :status)) ", nativeQuery = true)
    Page<Project> findProjects(@Param("name") String name, String status,
                               Pageable pageable);

    List<Project> findAll();

}
