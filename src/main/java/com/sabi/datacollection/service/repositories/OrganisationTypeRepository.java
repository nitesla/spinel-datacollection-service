package com.sabi.datacollection.service.repositories;

import com.sabi.datacollection.core.models.LGA;
import com.sabi.datacollection.core.models.OrganisationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrganisationTypeRepository extends JpaRepository<OrganisationType, Long> {
    OrganisationType findOrganisationTypeById(Long id);
    OrganisationType findOrganisationTypeByName(String name);

    List<OrganisationType> findOrganisationTypeByIsActive(Boolean isActive);

    @Query("SELECT o FROM OrganisationType o WHERE ((:name IS NULL) OR (:name IS NOT NULL AND o.name like %:name%))" +
            " AND ((:description IS NULL) OR (:description IS NOT NULL AND o.description like %:description%)) order by o.id desc")
    Page<OrganisationType> findOrganisationTypes(@Param("name") String name,
                                                   @Param("description") String description,
                                                   Pageable pageable);
}
