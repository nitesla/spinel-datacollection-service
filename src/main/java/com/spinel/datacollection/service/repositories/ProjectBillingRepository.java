package com.spinel.datacollection.service.repositories;


import com.spinel.datacollection.core.models.ProjectBilling;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectBillingRepository extends JpaRepository<ProjectBilling,Long> {


    List<ProjectBilling> findAllByIsActive(boolean isActive);

    List<ProjectBilling> findAllByProjectId(Long projectId);

    ProjectBilling findByProjectId(Long projectId);

    @Query("SELECT pb FROM ProjectBilling pb WHERE ((:projectId IS NULL ) OR (:projectId IS NOT NULL AND pb.projectId =:projectId)) " +
            "ORDER BY pb.id DESC ")
    Page<ProjectBilling> findProjectBillings(Long projectId,Pageable pageable);
}
