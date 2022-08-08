package com.spinel.datacollection.service.repositories;


import com.spinel.datacollection.core.models.ProjectOwnerEnumerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectOwnerEnumeratorRepository extends JpaRepository<ProjectOwnerEnumerator, Long> {
    ProjectOwnerEnumerator findProjectOwnerEnumeratorByProjectOwnerIdAndEnumeratorId(Long projectOwnerId, Long EnumeratorId);

    List<ProjectOwnerEnumerator> findProjectOwnerEnumeratorByProjectOwnerId(Long projectOwnerId);

    Page<ProjectOwnerEnumerator> findProjectOwnerEnumeratorByProjectOwnerId(Long projectOwnerId, Pageable pageable);

    Page<ProjectOwnerEnumerator> findProjectOwnerEnumeratorByEnumeratorId(Long enumeratorId, Pageable pageable);
}
