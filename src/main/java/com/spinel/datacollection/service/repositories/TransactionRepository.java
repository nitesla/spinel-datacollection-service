package com.spinel.datacollection.service.repositories;


import com.spinel.datacollection.core.models.Transaction;
import com.sabi.datacollection.core.enums.ActionType;
import com.sabi.datacollection.core.enums.Status;
import com.sabi.datacollection.core.enums.TransactionType;
import com.sabi.datacollection.core.models.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 *
 * This interface is responsible for Transaction crud operations
 */

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Transaction findTransactionById(Long Id);


    List<Transaction> findByIsActive(Boolean isActive);

    Transaction findByHash(String hash);

    @Query("SELECT s FROM Transaction s WHERE ((:walletId IS NULL) OR (:walletId IS NOT NULL AND s.walletId = :walletId))" +
            " AND ((:amount IS NULL) OR (:amount IS NOT NULL AND s.amount = :amount))" +
            " AND ((:initialBalance IS NULL) OR (:initialBalance IS NOT NULL AND s.initialBalance = :initialBalance))" +
            " AND ((:finalBalance IS NULL) OR (:finalBalance IS NOT NULL AND s.finalBalance = :finalBalance))" +
            " AND ((:actionType IS NULL) OR (:actionType IS NOT NULL AND s.actionType = :actionType))" +
            " AND ((:transactionType IS NULL) OR (:transactionType IS NOT NULL AND s.transactionType = :transactionType))" +
            " AND ((:status IS NULL) OR (:status IS NOT NULL AND s.status = :status))" +
            "AND ((:fromDateTime IS NULL) OR ((:fromDateTime IS NOT NULL) AND (s.createdDate >= :fromDateTime)))" +
            "AND ((:toDateTime IS NULL) OR ((:toDateTime IS NOT NULL) AND (s.createdDate <= :toDateTime)))" +
            " AND ((:reference IS NULL) OR (:reference IS NOT NULL AND s.reference like %:reference%)) order by s.id desc")
    Page<Transaction> findTransactions(@Param("walletId") Long walletId,
                                       @Param("amount") BigDecimal amount,
                                       @Param("initialBalance") BigDecimal initialBalance,
                                       @Param("finalBalance") BigDecimal finalBalance,
                                       ActionType actionType,
                                       TransactionType transactionType,
                                       Status status,
                                       @Param("reference") String reference,
                                       LocalDateTime fromDateTime,
                                       LocalDateTime toDateTime,
                                       Pageable pageable);

}
