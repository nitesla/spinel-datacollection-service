package com.spinel.datacollection.service.repositories;


import com.spinel.datacollection.core.enums.EnumeratorStatus;
import com.spinel.datacollection.core.models.Enumerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface EnumeratorRepository extends JpaRepository<Enumerator, Long>, JpaSpecificationExecutor<Enumerator> {

    Enumerator findByUserId(Long userId);

    Enumerator findEnumeratorById(Long id);

    Enumerator findEnumeratorByUserId(Long userId);

    List<Enumerator> findByIsActive(Boolean isActive);
    List<Enumerator> findEnumeratorByIsActive(Boolean isActive);

    List<Enumerator> findAll();

    Page<Enumerator> findByIsActive(Boolean isActive, Pageable pageable);

    Integer countAllByIsActive(Boolean isActive);

    List<Enumerator> findEnumeratorByVerificationStatus(String verificationStatus);
    Integer countAllByStatus(EnumeratorStatus status);


    @Query("SELECT p FROM Enumerator p WHERE ((:corporateName IS NULL) OR (:corporateName IS NOT NULL AND p.corporateName like %:corporateName%)) order by p.id desc")
    Page<Enumerator> findEnumeratorsProperties(@Param("corporateName") String corporateName, Pageable pageable);

}
