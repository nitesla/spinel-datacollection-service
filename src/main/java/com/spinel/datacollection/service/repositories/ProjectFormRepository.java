package com.spinel.datacollection.service.repositories;


import com.spinel.datacollection.core.models.ProjectForm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectFormRepository extends JpaRepository<ProjectForm, Long> {

    List<ProjectForm> findAllByIsActive(boolean isActive);

    List<ProjectForm> findAllByProjectId(Long projectId);

    List<ProjectForm> findAllByFormId(Long formId);

    ProjectForm findByProjectIdAndFormId(Long projectId, Long formId);

    @Query("SELECT pf FROM ProjectForm pf WHERE ((:projectId IS NULL ) OR (:projectId  IS NOT NULL AND pf.projectId =:projectId))" +
            "AND ((:formId IS NULL ) OR (:formId IS NOT NULL AND pf.formId =:formId)) ORDER BY pf.id DESC ")
    Page<ProjectForm> searchProjectForms(Long projectId, Long formId, Pageable pageable);
}
