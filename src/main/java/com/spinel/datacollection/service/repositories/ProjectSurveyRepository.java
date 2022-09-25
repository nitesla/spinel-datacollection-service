package com.spinel.datacollection.service.repositories;

import com.spinel.datacollection.core.models.ProjectSurvey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectSurveyRepository extends JpaRepository<ProjectSurvey, Long> {
    List<ProjectSurvey> findByProjectId(Long projectId);
}
