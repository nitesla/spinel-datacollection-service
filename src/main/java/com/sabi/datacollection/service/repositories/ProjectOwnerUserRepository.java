package com.sabi.datacollection.service.repositories;


import com.sabi.datacollection.core.models.ProjectOwnerUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ProjectOwnerUserRepository extends JpaRepository<ProjectOwnerUser, Long> {
    Page<ProjectOwnerUser> findProjectOwnerUserByUserId(Long userId, Pageable pageable);

    Page<ProjectOwnerUser> findProjectOwnerUserByProjectOwnerId(Long projectOwnerId, Pageable pageable);

    ProjectOwnerUser findProjectOwnerUserByUserIdAndProjectOwnerId(Long userId, Long projectOwnerId);
}
