package com.sabi.datacollection.service.repositories;


import com.sabi.datacollection.core.models.DataPermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface DataPermissionRepository extends JpaRepository<DataPermission, Long> {

    DataPermission findByName(String name);

    DataPermission findByNameAndAppPermission(String name , String appPermission);



    @Query("SELECT p FROM DataPermission p WHERE ((:name IS NULL) OR (:name IS NOT NULL AND p.name like %:name%)) " +
            " AND ((:appPermission IS NULL) OR (:appPermission IS NOT NULL AND p.appPermission = :appPermission)) order by p.id")
    Page<DataPermission> findFunctions(@Param("name")String name,
                                   @Param("appPermission")String appPermission,
                                   Pageable pageable);



    @Query("SELECT p FROM DataPermission p WHERE ((:name IS NULL) OR (:name IS NOT NULL AND p.name like %:name%)) " +
            " AND ((:appPermission IS NULL) OR (:appPermission IS NOT NULL AND p.appPermission = :appPermission)) order by p.id")
    List<DataPermission> listPermission(@Param("name")String name,
                                    @Param("appPermission")String appPermission);




    @Query(value ="SELECT p.name,p.appPermission FROM DataPermission p  INNER JOIN DataRolePermission rp  ON p.id = rp.permissionId\n" +
            "      INNER JOIN UserRole ur  ON rp.roleId = ur.roleId\n" +
            "    WHERE ur.userId =?1")
    List<Object[]> getPermissionsByUserId(Long userId);

}
