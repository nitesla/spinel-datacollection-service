package com.sabi.datacollection.service.repositories;

import com.sabi.datacollection.core.models.DataSet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataSetRepository extends JpaRepository<DataSet, Long> {
    DataSet findByName(String name);

    List<DataSet> findByIsActive(Boolean isActive);

    @Query("SELECT d FROM DataSet d WHERE ((:name IS NULL) OR (:name IS NOT NULL AND d.name like %:name%)) order by d.id desc")
    Page<DataSet> findDataSets(@Param("name") String name, Pageable pageable);
}
