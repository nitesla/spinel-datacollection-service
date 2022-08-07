package com.spinel.datacollection.service.repositories;


import com.spinel.datacollection.core.models.ProjectIndicator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectIndicatorRepository extends JpaRepository<ProjectIndicator, Long> {
    ProjectIndicator findProjectIndicatorByProjectIdAndIndicatorId(Long projectId, Long indicatorId);

    Page<ProjectIndicator> findProjectIndicatorByProjectId(Long projectId, Pageable pageable);
    Page<ProjectIndicator> findProjectIndicatorByIndicatorId(Long indicatorId, Pageable pageable);

    Page<ProjectIndicator> findAll(Pageable pageable);

    Page<ProjectIndicator> findProjectIndicatorByIsActive(Boolean isActive, Pageable pageable);
}
