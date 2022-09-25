package com.spinel.datacollection.service.repositories;

import com.spinel.datacollection.core.models.ProjectProjectCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectProjectCategoryRepository extends JpaRepository<ProjectProjectCategory, Long> {
    List<ProjectProjectCategory> findByProjectId(Long projectId);
}
