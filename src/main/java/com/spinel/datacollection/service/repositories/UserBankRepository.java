package com.spinel.datacollection.service.repositories;

import com.sabi.datacollection.core.models.UserBank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserBankRepository extends JpaRepository<UserBank, Long> {

    @Query("SELECT o FROM UserBank o WHERE ((:userId IS NULL) OR (:userId IS NOT NULL AND o.userId = userId)) " +
            "AND ((:bankId IS NULL) OR (:bankId IS NOT NULL AND o.bankId = :bankId)) " +
            "AND ((:accountNumber IS NULL) OR (:accountNumber IS NOT NULL AND o.accountNumber = :accountNumber))order by o.id desc")
    Page<UserBank> findUserBanks(@Param("userId") Long userId,
                                 @Param("bankId") Long bankId,
                                 @Param("accountNumber") String accountNumber,
                                 Pageable pageRequest);

    List<UserBank> findByIsActive(Boolean isActive);
}
