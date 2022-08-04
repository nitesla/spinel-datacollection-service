package com.sabi.datacollection.service.repositories;

import com.sabi.datacollection.core.models.ProjectRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRoleRepository extends JpaRepository<ProjectRole, Long> {

    Optional<ProjectRole> findById(Long id);

    ProjectRole findByName(String name);

    List<ProjectRole> findByIsActive(Boolean isActive);

    @Query("SELECT pr FROM ProjectRole pr WHERE ((:name IS NULL) OR (:name IS NOT NULL AND pr.name like %:name%)) order by pr.id desc")
    Page<ProjectRole> findProjectRoles(@Param("name") String name, Pageable pageable);

}
