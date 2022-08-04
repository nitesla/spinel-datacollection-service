package com.sabi.datacollection.service.services;

import com.sabi.datacollection.core.dto.request.PaymentRequestDto;
import com.sabi.datacollection.core.dto.response.PaymentResponseDto;
import com.sabi.datacollection.core.models.Payment;
import com.sabi.datacollection.service.helper.Validations;
import com.sabi.datacollection.service.repositories.DataPaymentRepository;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DataPaymentService {

    private final DataPaymentRepository dataPaymentRepository;
    private final ModelMapper mapper;
    private final Validations validations;


    public DataPaymentService(DataPaymentRepository dataPaymentRepository, ModelMapper mapper, Validations validations) {
        this.dataPaymentRepository = dataPaymentRepository;
        this.mapper = mapper;
        this.validations = validations;
    }

    public PaymentResponseDto createPayment(PaymentRequestDto request) {
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Payment payment = mapper.map(request, Payment.class);
        payment.setReference(validations.generateReferenceNumber(10));
        payment = dataPaymentRepository.save(payment);
        log.info("Created new Payment - {}", payment);
        return mapper.map(payment, PaymentResponseDto.class);
    }

    public PaymentResponseDto findPaymentById(Long id) {
        Payment payment = dataPaymentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Payment Id does not exist"));
        return mapper.map(payment, PaymentResponseDto.class);
    }

    public Page<Payment> findAll(String paymentReference, String reference, String status, Integer paymentMethod, Pageable pageable) {
        return dataPaymentRepository.findAll(paymentReference, reference,status,
                paymentMethod, pageable);
    }



}
