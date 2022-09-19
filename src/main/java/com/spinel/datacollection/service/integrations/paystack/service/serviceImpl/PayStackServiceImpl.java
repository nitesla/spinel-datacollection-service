package com.spinel.datacollection.service.integrations.paystack.service.serviceImpl;


import com.spinel.datacollection.core.dto.payment.request.InitializeTransactionRequest;
import com.spinel.datacollection.core.dto.payment.request.VerifyTransaction;
import com.spinel.datacollection.core.dto.payment.response.InitializeTransactionResponse;
import com.spinel.datacollection.core.dto.payment.response.VerifyTransactionResponse;
import com.spinel.datacollection.core.integrations.paystack.dto.response.PayStackInitializeTransactionDataResponse;
import com.spinel.datacollection.core.integrations.paystack.dto.response.PayStackResponse;
import com.spinel.datacollection.core.integrations.paystack.dto.response.PayStackVerifyTransactionResponse;
import com.spinel.datacollection.service.integrations.paystack.enums.PayStackCurrency;
import com.spinel.datacollection.service.payment.PaymentService;
import com.spinel.framework.helpers.API;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;


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
    public InitializeTransactionResponse initializeTransaction(InitializeTransactionRequest initializeTransaction) {
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
    public VerifyTransactionResponse verifyTransaction(VerifyTransaction verifyTransaction) {
        String url = verifyTransactionUrl + verifyTransaction.getReference();
        PayStackVerifyTransactionResponse payStackResponse = api.get(url, PayStackVerifyTransactionResponse.class, getHeader());

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH);

        return VerifyTransactionResponse.builder()
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

//    @Override
//    public PayStackResponse listTransaction(int perPage, int page, String from, String to, String status, String customer) {
//        if(Objects.nonNull(status)) {
//            PayStackTransactionStatus.isValidPayStackTransactionStatus(status);
//        }
//        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(listTransactionUrl)
//                .queryParam("perPage", perPage)
//                .queryParam("page", page)
//                .queryParam("from", from)
//                .queryParam("to", to)
//                .queryParam("status", status)
//                .queryParam("customer", customer);
//
//        return api.get(builder.toUriString(), PayStackResponse.class, getHeader());
//    }
//
//    @Override
//    public PayStackResponse fetchTransaction(String transactionId) {
//        String url = fetchTransactionUrl + Integer.parseInt(transactionId);
//        return api.get(url, PayStackResponse.class, getHeader());
//    }
//
//    @Override
//    public PayStackResponse totalTransactions(int perPage, int page, String from, String to) {
//        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(totalTransactionUrl)
//                .queryParam("perPage", perPage)
//                .queryParam("page", page)
//                .queryParam("from", from)
//                .queryParam("to", to);
//
//        return api.get(builder.toUriString(), PayStackResponse.class, getHeader());
//    }
//
    private Map<String, String> getHeader() {
        Map<String, String> map = new HashMap<>();
        map.put("Content-type", "application/json");
        map.put("Authorization", "Bearer " + secretKey);
        return map;
    }
}
