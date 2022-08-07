package com.spinel.datacollection.service.repositories;


import com.spinel.datacollection.core.models.Sector;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SectorRepository extends JpaRepository<Sector, Long> {
    Sector findByName(String name);

    List<Sector> findByIsActive(Boolean isActive);

    @Query("SELECT s FROM Sector s WHERE ((:name IS NULL) OR (:name IS NOT NULL AND s.name like %:name%)) order by s.id desc")
    Page<Sector> findSectors(@Param("name") String name, Pageable pageable);
}
