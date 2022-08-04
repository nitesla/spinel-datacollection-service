package com.sabi.datacollection.service.repositories;

import com.sabi.datacollection.core.models.IndicatorDictionary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IndicatorDictionaryRepository extends JpaRepository<IndicatorDictionary, Long> {
    IndicatorDictionary findByName(String name);

    List<IndicatorDictionary> findByIsActive(Boolean isActive);

    @Query("SELECT i FROM IndicatorDictionary i WHERE ((:name IS NULL) OR (:name IS NOT NULL AND i.name like %:name%)) order by i.id desc")
    Page<IndicatorDictionary> findIndicatorDictionaries(@Param("name") String name, Pageable pageable);

}
