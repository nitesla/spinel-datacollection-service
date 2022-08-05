package com.spinel.datacollection.service.repositories;



import com.spinel.datacollection.core.models.ProjectCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectCategoryRepository extends JpaRepository<ProjectCategory, Long> {
    ProjectCategory findByName(String name);

    List<ProjectCategory> findByIsActive(Boolean isActive);

    @Query("SELECT p FROM ProjectCategory p WHERE ((:name IS NULL) OR (:name IS NOT NULL AND p.name like %:name%))" +
            " AND ((:description IS NULL) OR (:description IS NOT NULL AND p.description like %:description%)) order by p.id desc")
    Page<ProjectCategory> findProjectCategories(@Param("name") String name,
                                                @Param("description") String description,
                                                Pageable pageable);

}
