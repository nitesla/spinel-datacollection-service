package com.spinel.datacollection.service.repositories;

import com.spinel.datacollection.core.models.ProjectFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectFileRepository extends JpaRepository<ProjectFile, Long> {
    List<ProjectFile> findByProjectId(Long projectId);
}
