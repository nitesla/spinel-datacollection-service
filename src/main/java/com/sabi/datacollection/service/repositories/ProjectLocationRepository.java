package com.sabi.datacollection.service.repositories;


import com.sabi.datacollection.core.models.ProjectLocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectLocationRepository extends JpaRepository<ProjectLocation, Long> {
    ProjectLocation findProjectLocationByName(String name);

    List<ProjectLocation> findProjectLocationByIsActive(Boolean isActive);

    Page<ProjectLocation> findProjectLocationByLocationId(Long id, Pageable pageable);

    Page<ProjectLocation> findProjectLocationByLocationType(String  location, Pageable pageable);

    @Query("SELECT p FROM ProjectLocation p WHERE ((:name IS NULL) OR (:name IS NOT NULL AND p.name like %:name%)) " +
            "AND ((:locationType IS NULL) OR (:locationType IS NOT NULL AND p.locationType like  %:locationType%)) order by p.id desc")
    Page<ProjectLocation> findProjectLocations(@Param("name") String name,
                               @Param("locationType") String locationType,
                               Pageable pageable);
}
