package com.sabi.datacollection.service.repositories;


import com.sabi.datacollection.core.models.DataRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@SuppressWarnings("ALL")
@Repository
public interface DataRoleRepository extends JpaRepository<DataRole, Long> {
    DataRole findByName(String name);

    @Query("SELECT r FROM DataRole r WHERE ((:name IS NULL) OR (:name IS NOT NULL AND r.name like %:name%))" +
            " AND ((:status IS NULL) OR (:status IS NOT NULL AND r.status = :status)) order by r.id")
    Page<DataRole> findRoles(@Param("name")String name,
                         @Param("status")Integer status,
                         Pageable pageable);



}
