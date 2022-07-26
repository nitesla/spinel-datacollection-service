package com.sabi.datacollection.service.repositories;

import com.sabi.datacollection.core.enums.Status;
import com.sabi.datacollection.core.models.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    Project findByName(String name);

    List<Project> findByStatus(Status status);

    List<Project> findByStatusAndProjectCategoryId(Status status, Long categoryId);

    List<Project> findByProjectCategoryId(Long categoryId);

    List<Project> findByProjectOwnerId(Long projectOwnerId);

    List<Project> findByIsActive(Boolean isActive);

    @Query("SELECT p FROM Project p WHERE ((:name IS NULL) OR (:name IS NOT NULL AND p.name like %:name%)) order by p.id desc")
    Page<Project> findProjects(@Param("name") String name,
                                                Pageable pageable);

    List<Project> findAll();
}
