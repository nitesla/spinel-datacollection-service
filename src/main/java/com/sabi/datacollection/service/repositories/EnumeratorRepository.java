package com.sabi.datacollection.service.repositories;



import com.sabi.datacollection.core.models.Enumerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface EnumeratorRepository extends JpaRepository<Enumerator, Long> {

    Enumerator findByUserId(Long userId);

    Enumerator findEnumeratorById(Long id);

    List<Enumerator> findByIsActive(Boolean isActive);

    Page<Enumerator> findByIsActive(Boolean isActive, Pageable pageable);

    @Query("SELECT p FROM Enumerator p WHERE ((:corporateName IS NULL) OR (:corporateName IS NOT NULL AND p.corporateName like %:corporateName%)) order by p.id desc")
    Page<Enumerator> findEnumeratorsProperties(@Param("corporateName") String corporateName, Pageable pageable);

}
