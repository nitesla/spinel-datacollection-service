package com.spinel.datacollection.service.repositories;



import com.spinel.datacollection.core.models.EnumeratorRating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnumeratorRatingRepository extends JpaRepository<EnumeratorRating, Long> {
    EnumeratorRating findByEnumeratorProjectId(Long enumeratorProjectId);

    List<EnumeratorRating> findByIsActive(Boolean isActive);

    @Query("SELECT e FROM EnumeratorRating e WHERE ((:enumeratorProjectId IS NULL) OR (:enumeratorProjectId IS NOT NULL AND e.enumeratorProjectId = :enumeratorProjectId))" +
            " AND ((:rating IS NULL) OR (:rating IS NOT NULL AND e.rating = :rating)) order by e.id desc")
    Page<EnumeratorRating> findEnumeratorRatings(@Param("enumeratorProjectId") Long enumeratorProjectId,
                                                 @Param("rating") Integer rating,
                                                 Pageable pageable);
}

