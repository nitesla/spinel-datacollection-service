package com.spinel.datacollection.service.repositories;



import com.spinel.datacollection.core.models.EnumeratorProjectLocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnumeratorProjectLocRepository extends JpaRepository<EnumeratorProjectLocation, Long> {
    EnumeratorProjectLocation findByEnumeratorProjectIdAndProjectLocationId(Long enumeratorProjectId, Long projectLocationId);

    List<EnumeratorProjectLocation> findByIsActive(Boolean isActive);

    @Query("SELECT e FROM EnumeratorProjectLocation e WHERE ((:enumeratorProjectId IS NULL) OR (:enumeratorProjectId IS NOT NULL AND e.enumeratorProjectId = :enumeratorProjectId))" +
            " AND ((:projectLocationId IS NULL) OR (:projectLocationId IS NOT NULL AND e.projectLocationId = :projectLocationId)) " +
            " AND ((:collectedRecord IS NULL) OR (:collectedRecord IS NOT NULL AND e.collectedRecord = :collectedRecord)) " +
            " AND ((:expectedRecord IS NULL) OR (:expectedRecord IS NOT NULL AND e.expectedRecord = :expectedRecord)) order by e.id desc")
    Page<EnumeratorProjectLocation> findEnumeratorProjectLocations(@Param("enumeratorProjectId") Long enumeratorProjectId,
                                                   @Param("projectLocationId") Long projectLocationId,
                                                   @Param("collectedRecord") Integer collectedRecord,
                                                   @Param("expectedRecord") Integer expectedRecord, Pageable pageable);
}

