package com.spinel.datacollection.service.repositories;






import com.spinel.datacollection.core.models.Form;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 *
 * This interface is responsible for Form crud operations
 */

@Repository
public interface FormRepository extends JpaRepository<Form, Long> {

    Form findByName(String name);

    Form findFormById(Long Id);

    @Query("SELECT s FROM Form s WHERE ((:isActive IS NULL) OR (:isActive IS NOT NULL AND s.isActive = :isActive))"  +
            " AND ((:projectId IS NULL) OR (:projectId IS NOT NULL AND s.projectId = :projectId))" +
            " AND ((:projectOwnerId IS NULL) OR (:projectOwnerId IS NOT NULL AND s.projectOwnerId = :projectOwnerId)) order by s.id desc")
    List<Form> findByIsActive(Boolean isActive, Long projectId, Long projectOwnerId);


    @Query("SELECT s FROM Form s WHERE ((:name IS NULL) OR (:name IS NOT NULL AND s.name like %:name%))" +
            " AND ((:version IS NULL) OR (:version IS NOT NULL AND s.version like %:version%))" +
            " AND ((:description IS NULL) OR (:description IS NOT NULL AND s.description like %:description%)) order by s.id desc")
    Page<Form> findForms(@Param("name") String name,
                         @Param("version") String version,
                         @Param("description") String description,
                         Pageable pageable);

}
