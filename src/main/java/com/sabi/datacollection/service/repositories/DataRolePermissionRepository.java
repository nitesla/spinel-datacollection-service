package com.sabi.datacollection.service.repositories;



import com.sabi.datacollection.core.models.DataRolePermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface DataRolePermissionRepository extends JpaRepository<DataRolePermission, Long> {

    List<DataRolePermission> findAllByRoleId(Long roleId);
    DataRolePermission findByRoleIdAndPermissionId(Long roleId,Long permissionId);

    @Query("SELECT p FROM DataRolePermission p WHERE ((:roleId IS NULL) OR (:roleId IS NOT NULL AND p.roleId = :roleId)) " +
            " AND ((:status IS NULL) OR (:status IS NOT NULL AND p.status = :status)) order by p.id")
    Page<DataRolePermission> findRolePermission(@Param("roleId") Long roleId,
                                            @Param("status") int status,
                                            Pageable Pageable);

    @Query("SELECT rp FROM DataRolePermission rp WHERE rp.roleId=?1 order by rp.id" )
    List<DataRolePermission> getPermissionsByRole(Long roleId);

}
