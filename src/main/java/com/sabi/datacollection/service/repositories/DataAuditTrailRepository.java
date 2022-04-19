package com.sabi.datacollection.service.repositories;

import com.sabi.datacollection.core.models.DataAuditTrail;
import com.sabi.datacollection.core.models.DataPermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface DataAuditTrailRepository extends JpaRepository<DataAuditTrail, Long> {

    @Query("select a from DataAuditTrail a where ((:username IS NULL) OR (:username IS NOT NULL AND a.username like %:username%)) " +
            " AND ((:event IS NULL) OR (:event IS NOT NULL AND a.event = :event))"+
            " AND ((:flag IS NULL) OR (:flag IS NOT NULL AND a.flag = :flag))"+
            "AND ((:startDate IS NULL) OR (:startDate IS NOT NULL AND a.requestTime >= :startDate)) " +
            "AND ((:endDate IS NULL) OR (:endDate IS NOT NULL AND  a.requestTime <= :endDate)) order by a.id DESC")
    Page<DataAuditTrail> audits(@Param("username") String username,
                            @Param("event") String event,
                            @Param("flag") String flag,
                            @Param("startDate") LocalDateTime startDate,
                            @Param("endDate") LocalDateTime endDate,
                            Pageable pageable);

}
