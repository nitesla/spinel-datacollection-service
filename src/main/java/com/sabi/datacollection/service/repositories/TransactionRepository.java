package com.sabi.datacollection.service.repositories;

import com.sabi.datacollection.core.models.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 *
 * This interface is responsible for Transaction crud operations
 */

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Transaction findTransactionById(Long Id);


    List<Transaction> findByIsActive(Boolean isActive);


    @Query("SELECT s FROM Transaction s WHERE ((:walletId IS NULL) OR (:walletId IS NOT NULL AND s.walletId = :walletId))" +
            " AND ((:amount IS NULL) OR (:amount IS NOT NULL AND s.amount = :amount))" +
            " AND ((:reference IS NULL) OR (:reference IS NOT NULL AND s.reference like %:reference%)) order by s.id desc")
    Page<Transaction> findTransactions(@Param("walletId") Long walletId,
                                       @Param("amount") BigDecimal amount,
                                       @Param("reference") String reference,
                                       Pageable pageable);

}
