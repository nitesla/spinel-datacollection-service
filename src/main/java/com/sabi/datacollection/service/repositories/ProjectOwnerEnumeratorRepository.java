package com.sabi.datacollection.service.repositories;

import com.sabi.datacollection.core.models.ProjectOwnerEnumerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectOwnerEnumeratorRepository extends JpaRepository<ProjectOwnerEnumerator, Long> {
    ProjectOwnerEnumerator findProjectOwnerEnumeratorByProjectOwnerIdAndEnumeratorId(Long projectOwnerId, Long EnumeratorId);

    Page<ProjectOwnerEnumerator> findProjectOwnerEnumeratorByProjectOwnerId(Long projectOwnerId, Pageable pageable);

    Page<ProjectOwnerEnumerator> findProjectOwnerEnumeratorByEnumeratorId(Long enumeratorId, Pageable pageable);
}
