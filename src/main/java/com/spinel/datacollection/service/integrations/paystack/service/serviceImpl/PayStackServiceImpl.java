package com.spinel.datacollection.service.integrations.paystack.service.serviceImpl;


import com.spinel.datacollection.core.dto.payment.request.*;
import com.spinel.datacollection.core.dto.payment.response.InitializeTransactionResponse;
import com.spinel.datacollection.core.dto.payment.response.TotalTransactionResponse;
import com.spinel.datacollection.core.dto.payment.response.TransactionResponse;
import com.spinel.datacollection.core.integrations.paystack.dto.response.*;
import com.spinel.datacollection.service.integrations.paystack.enums.PayStackCurrency;
import com.spinel.datacollection.service.integrations.paystack.enums.PayStackTransactionStatus;
import com.spinel.datacollection.service.payment.PaymentService;
import com.spinel.framework.helpers.API;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Slf4j
@RequiredArgsConstructor
@Service
public class PayStackServiceImpl implements PaymentService {

    @Value("${paystack.secret.key}")
    private String secretKey;

    @Value("${paystack.initialize.transaction.url}")
    private String initializeTransactionUrl;

    @Value("${paystack.verify.transaction.url}")
    private String verifyTransactionUrl;

    @Value("${paystack.list.transaction.url}")
    private String listTransactionUrl;

    @Value("${paystack.fetch.transaction.url}")
    private String fetchTransactionUrl;

    @Value("${paystack.total.transaction.url}")
    private String totalTransactionUrl;

    private final API api;
    private final ModelMapper mapper;


    @Override
    public InitializeTransactionResponse initializeTransaction(InitializeTransaction initializeTransaction) {
        if(Objects.nonNull(initializeTransaction.getCurrency())){
            PayStackCurrency.isValidPayStackCurrency(initializeTransaction.getCurrency());
        }


        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("amount", initializeTransaction.getAmount().toString());
        requestBody.put("email", initializeTransaction.getEmail());
        requestBody.put("currency", initializeTransaction.getCurrency());
        requestBody.put("reference", initializeTransaction.getReference());



        PayStackResponse response = api.post(initializeTransactionUrl, requestBody, PayStackResponse.class, getHeader());
        PayStackInitializeTransactionDataResponse dataResponse = mapper.map(response.getData(), PayStackInitializeTransactionDataResponse.class);
        InitializeTransactionResponse initializeTransactionResponse = new InitializeTransactionResponse();
        initializeTransactionResponse.setReference(dataResponse.getReference());
        initializeTransactionResponse.setMessage(response.getMessage());
        initializeTransactionResponse.setStatus(response.getStatus());
        initializeTransactionResponse.setAccessCode(dataResponse.getAccess_code());
        initializeTransactionResponse.setUrl(dataResponse.getAuthorization_url());
        return initializeTransactionResponse;
    }

    @Override
    public TransactionResponse verifyTransaction(VerifyTransaction verifyTransaction) {
        String url = verifyTransactionUrl + verifyTransaction.getReference();
        PayStackVerifyTransactionResponse payStackResponse = api.get(url, PayStackVerifyTransactionResponse.class, getHeader());

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH);

        return TransactionResponse.builder()
                .paymentProviderId(payStackResponse.getData().getId())
                .customerID(payStackResponse.getData().getCustomer().getId())
                .status(payStackResponse.getStatus())
                .reference(verifyTransaction.getReference())
                .amount(payStackResponse.getData().getAmount())
                .createdAt(LocalDateTime.parse(payStackResponse.getData().getCreatedAt(), formatter))
                .email(payStackResponse.getData().getCustomer().getEmail())
                .message(payStackResponse.getMessage())
                .build();
    }

    @Override
    public List<TransactionResponse> listTransactions(ListTransactions listTransactions) {
        if(Objects.nonNull(listTransactions.getStatus())) {
            PayStackTransactionStatus.isValidPayStackTransactionStatus(listTransactions.getStatus());
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(listTransactionUrl)
                .queryParam("perPage", listTransactions.getPerPage())
                .queryParam("page", listTransactions.getPage())
                .queryParam("from", listTransactions.getFrom())
                .queryParam("to", listTransactions.getTo())
                .queryParam("status", listTransactions.getStatus())
                .queryParam("customer", listTransactions.getCustomer());

        PayStackListTransactionResponse payStackResponse = api.get(builder.toUriString(), PayStackListTransactionResponse.class, getHeader());
        List<TransactionResponse> response = new ArrayList<>();
        for (PayStackVerifyTransactionDataResponse dataResponse: payStackResponse.getData()) {
            TransactionResponse transactionResponse = mapper.map(dataResponse, TransactionResponse.class);
            transactionResponse.setPaymentProviderId(dataResponse.getId());
            response.add(transactionResponse);
        }
        return response;
    }

    @Override
    public TransactionResponse fetchTransaction(String transactionId) {
        String url = fetchTransactionUrl + Integer.parseInt(transactionId);
        PayStackFetchTransactionResponse fetchTransactionResponse = api.get(url, PayStackFetchTransactionResponse.class, getHeader());
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH);

        return TransactionResponse.builder()
                .paymentProviderId(fetchTransactionResponse.getData().getId())
                .customerID(fetchTransactionResponse.getData().getCustomer().getId())
                .status(fetchTransactionResponse.getStatus())
                .reference(fetchTransactionResponse.getData().getReference())
                .amount(fetchTransactionResponse.getData().getAmount())
                .createdAt(LocalDateTime.parse(fetchTransactionResponse.getData().getCreatedAt(), formatter))
                .email(fetchTransactionResponse.getData().getCustomer().getEmail())
                .message(fetchTransactionResponse.getMessage())
                .build();
    }

    @Override
    public TotalTransactionResponse totalTransactions(TotalTransaction totalTransaction) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(totalTransactionUrl)
                .queryParam("perPage", totalTransaction.getPerPage())
                .queryParam("page", totalTransaction.getPage())
                .queryParam("from", totalTransaction.getFrom())
                .queryParam("to", totalTransaction.getTo());

        return api.get(builder.toUriString(), TotalTransactionResponse.class, getHeader());
    }

    private Map<String, String> getHeader() {
        Map<String, String> map = new HashMap<>();
        map.put("Content-type", "application/json");
        map.put("Authorization", "Bearer " + secretKey);
        return map;
    }
}
