package com.spinel.datacollection.service.services;


import com.spinel.datacollection.core.dto.payment.request.*;
import com.spinel.datacollection.core.dto.payment.response.TotalTransactionResponse;
import com.spinel.datacollection.core.dto.payment.response.TransactionResponse;
import com.spinel.datacollection.core.dto.response.PaymentResponseDto;
import com.spinel.datacollection.core.dto.payment.response.InitializeTransactionResponse;
import com.spinel.datacollection.core.models.Payment;
import com.spinel.datacollection.service.helper.IntegratedPaymentService;
import com.spinel.datacollection.service.helper.Validations;
import com.spinel.datacollection.service.payment.PaymentFactoryService;
import com.spinel.datacollection.service.payment.PaymentService;
import com.spinel.datacollection.service.repositories.DataPaymentRepository;
import com.spinel.framework.exceptions.ConflictException;
import com.spinel.framework.utils.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Random;

@SuppressWarnings("ALL")
@RequiredArgsConstructor
@Slf4j
@Service
public class DataPaymentService {

    private final PaymentFactoryService factoryService;
    private final DataPaymentRepository dataPaymentRepository;
    private final ModelMapper mapper;
    private final Validations validations;

    public Page<Payment> findAll(String paymentReference, String reference, String status, Integer paymentMethod, Pageable pageable) {
        return dataPaymentRepository.findAll(paymentReference, reference,status,
                paymentMethod, pageable);
    }

    public PaymentResponseDto findByReference(String reference) {
        Payment payment = dataPaymentRepository.findByReference(reference);
        return mapper.map(payment, PaymentResponseDto.class);
    }

    public PaymentResponseDto initializeTransaction(InitializeTransactionRequest request) {
        String reference = generateReference();
        Payment paymentExists = dataPaymentRepository.findByReference(reference);
        if(Objects.nonNull(paymentExists)) {
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "reference number already exists");
        }
        InitializeTransaction initializeTransaction = mapper.map(request, InitializeTransaction.class);
        initializeTransaction.setReference(reference);
        IntegratedPaymentService integratedPaymentService = IntegratedPaymentService.validatePaymentService(initializeTransaction.getPaymentProvider().toLowerCase());
        PaymentService paymentService = factoryService.getPaymentService(integratedPaymentService);
        InitializeTransactionResponse response = paymentService.initializeTransaction(initializeTransaction);
        Payment payment = new Payment();
        payment.setAmount(response.getAmount());
        payment.setThirdPartyCode(response.getAccessCode());
        payment.setReference(reference);
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
        PaymentService paymentService = validatePaymentProvider(paymentExists.getPaymentProvider());
        TransactionResponse response = paymentService.verifyTransaction(verifyTransaction);
        paymentExists.setPaymentProviderId(response.getPaymentProviderId());
        paymentExists.setResponseDescription(response.getMessage());
        paymentExists.setTransactionDate(response.getCreatedAt());
        paymentExists.setAmount(response.getAmount());
        paymentExists.setStatus(response.getStatus());
        dataPaymentRepository.save(paymentExists);

        return mapper.map(paymentExists, PaymentResponseDto.class);
    }

    public TotalTransactionResponse totalTransactions(int perPage, int page, String from, String to, String paymentProvider) {
        PaymentService paymentService = validatePaymentProvider(paymentProvider);
        return paymentService.totalTransactions(new TotalTransaction(perPage, page, from, to));
    }

    public List<TransactionResponse> listTransactions(int perPage, int page, String from, String to, String status, String customer, String paymentProvider) {
        PaymentService paymentService = validatePaymentProvider(paymentProvider);
        return paymentService.listTransactions(new ListTransactions(perPage, page, from, to, status, customer));
    }

    public TransactionResponse fetchTransactions(String paymentProviderId, String paymentProvider) {
        PaymentService paymentService = validatePaymentProvider(paymentProvider);
        return paymentService.fetchTransaction(paymentProviderId);
    }

    private String generateReference() {
        String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++)
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return "TRN_REF"+sb.toString();
    }

    private PaymentService validatePaymentProvider(String paymentProvider) {
        IntegratedPaymentService integratedPaymentService = IntegratedPaymentService.validatePaymentService(paymentProvider.toLowerCase());
        return factoryService.getPaymentService(integratedPaymentService);
    }



}
