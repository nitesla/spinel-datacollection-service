package com.spinel.datacollection.service.services;


import com.spinel.datacollection.core.dto.payment.request.InitializeTransactionRequest;
import com.spinel.datacollection.core.dto.payment.request.VerifyTransaction;
import com.spinel.datacollection.core.dto.payment.response.VerifyTransactionResponse;
import com.spinel.datacollection.core.dto.request.PaymentRequestDto;
import com.spinel.datacollection.core.dto.response.PaymentResponseDto;
import com.spinel.datacollection.core.dto.payment.response.InitializeTransactionResponse;
import com.spinel.datacollection.core.models.Payment;
import com.spinel.datacollection.service.helper.IntegratedPaymentService;
import com.spinel.datacollection.service.helper.Validations;
import com.spinel.datacollection.service.payment.PaymentFactoryService;
import com.spinel.datacollection.service.payment.PaymentService;
import com.spinel.datacollection.service.repositories.DataPaymentRepository;
import com.spinel.framework.exceptions.ConflictException;
import com.spinel.framework.exceptions.NotFoundException;
import com.spinel.framework.models.User;
import com.spinel.framework.service.TokenService;
import com.spinel.framework.utils.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Objects;

@SuppressWarnings("ALL")
@RequiredArgsConstructor
@Slf4j
@Service
public class DataPaymentService {

    private final PaymentFactoryService factoryService;
    private final DataPaymentRepository dataPaymentRepository;
    private final ModelMapper mapper;
    private final Validations validations;


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

    public PaymentResponseDto findByReference(String reference) {
        Payment payment = dataPaymentRepository.findByReference(reference);
        return mapper.map(payment, PaymentResponseDto.class);
    }

    public PaymentResponseDto initializeTransaction(InitializeTransactionRequest initializeTransaction) {
        Payment paymentExists = dataPaymentRepository.findByReference(initializeTransaction.getReference());
        if(Objects.nonNull(paymentExists)) {
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "reference number already exists");
        }
        IntegratedPaymentService integratedPaymentService = IntegratedPaymentService.validatePaymentService(initializeTransaction.getPaymentProvider().toLowerCase());
        PaymentService paymentService = factoryService.getPaymentService(integratedPaymentService);
        InitializeTransactionResponse response = paymentService.initializeTransaction(initializeTransaction);
        Payment payment = new Payment();
        payment.setAmount(response.getAmount());
        payment.setThirdPartyCode(response.getAccessCode());
        payment.setReference(initializeTransaction.getReference());
        payment.setPaymentReference(response.getReference());
        payment.setRedirectURL(response.getUrl());
        payment.setPaymentProvider(integratedPaymentService.getValue());
        payment.setEmail(initializeTransaction.getEmail());
        dataPaymentRepository.save(payment);
        PaymentResponseDto paymentResponseDto = mapper.map(payment, PaymentResponseDto.class);
        paymentResponseDto.setResponseDescription(response.getMessage());
        return paymentResponseDto;
    }

    public PaymentResponseDto verifyTransaction(VerifyTransaction verifyTransaction) {
        Payment paymentExists = dataPaymentRepository.findByReference(verifyTransaction.getReference());
        if(Objects.isNull(paymentExists)) {
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "Invalid transaction reference.");
        }
        IntegratedPaymentService integratedPaymentService = IntegratedPaymentService.validatePaymentService(paymentExists.getPaymentProvider());
        PaymentService paymentService = factoryService.getPaymentService(integratedPaymentService);
        VerifyTransactionResponse response = paymentService.verifyTransaction(verifyTransaction);
        paymentExists.setPaymentProviderId(response.getPaymentProviderId());
        paymentExists.setResponseDescription(response.getMessage());
        paymentExists.setTransactionDate(response.getCreatedAt());
        paymentExists.setAmount(response.getAmount());
        paymentExists.setStatus(response.getStatus());
        dataPaymentRepository.save(paymentExists);

        return mapper.map(paymentExists, PaymentResponseDto.class);

    }



}
