package com.spinel.datacollection.service.repositories;

import com.spinel.datacollection.core.models.ProjectMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectMediaRepository extends JpaRepository<ProjectMedia, Long> {
    List<ProjectMedia> findByProjectId(Long projectId);
}
