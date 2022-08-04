package com.sabi.datacollection.service.repositories;

import com.sabi.datacollection.core.models.Payment;
import com.sabi.datacollection.core.models.Wallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataWalletRepository extends JpaRepository<Wallet, Long> {
    Wallet findByLastTransactionId(Long transactionId);
    Wallet findByUserId(Long userId);
    Wallet findByIdentificationNumber(String identificationNumber);
    List<Wallet> findByIsActive(Boolean isActive);
    Page<Wallet> findAll(Pageable pageable);
}
