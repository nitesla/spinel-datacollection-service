package com.spinel.datacollection.service.repositories;

import com.spinel.datacollection.core.models.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Map;

public interface DataPaymentRepository extends JpaRepository<Payment, Long> {

    @Query(value = "SELECT * FROM Payment WHERE " +
            "((:paymentReference IS NULL) OR (:paymentReference IS NOT NULL AND Payment.paymentReference = :paymentReference)) " +
            "AND ((:reference IS NULL) OR (:reference IS NOT NULL AND Payment.reference = :reference)) " +
            "AND ((:status IS NULL) OR (:status IS NOT NULL AND Payment.status = :status)) " +
            "AND ((:paymentMethod IS NULL) OR (:paymentMethod IS NOT NULL AND Payment.status = :paymentMethod))", nativeQuery = true)
    Page<Payment> findAll(@Param("paymentReference") String paymentReference,
                           @Param("reference") String reference,
                           @Param("status") String status,
                           @Param("paymentMethod") Integer paymentMethod,
                           Pageable pageable);
}
