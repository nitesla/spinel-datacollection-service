package com.spinel.datacollection.service.integrations.paystack.service.serviceImpl;

import com.spinel.datacollection.core.integrations.paystack.dto.request.InitializeTransaction;
import com.spinel.datacollection.core.integrations.paystack.dto.response.PayStackResponse;
import com.spinel.datacollection.service.integrations.paystack.enums.PayStackCurrency;
import com.spinel.datacollection.service.integrations.paystack.enums.PayStackTransactionStatus;
import com.spinel.datacollection.service.integrations.paystack.service.PayStackService;
import com.spinel.framework.helpers.API;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


@Slf4j
@RequiredArgsConstructor
@Service
public class PayStackServiceImpl implements PayStackService {

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

    @Override
    public PayStackResponse initializeTransaction(InitializeTransaction initializeTransaction) {
        if(Objects.nonNull(initializeTransaction.getCurrency())){
            PayStackCurrency.isValidPayStackCurrency(initializeTransaction.getCurrency());
        }
        return api.post(initializeTransactionUrl, initializeTransaction, PayStackResponse.class, getHeader());
    }

    @Override
    public PayStackResponse verifyTransaction(String reference) {
        String url = verifyTransactionUrl + reference;
        return api.get(url, PayStackResponse.class, getHeader());
    }

    @Override
    public PayStackResponse listTransaction(int perPage, int page, String from, String to, String status, String customer) {
        if(Objects.nonNull(status)) {
            PayStackTransactionStatus.isValidPayStackTransactionStatus(status);
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(listTransactionUrl)
                .queryParam("perPage", perPage)
                .queryParam("page", page)
                .queryParam("from", from)
                .queryParam("to", to)
                .queryParam("status", status)
                .queryParam("customer", customer);

        return api.get(builder.toUriString(), PayStackResponse.class, getHeader());
    }

    @Override
    public PayStackResponse fetchTransaction(String transactionId) {
        String url = fetchTransactionUrl + Integer.parseInt(transactionId);
        return api.get(url, PayStackResponse.class, getHeader());
    }

    @Override
    public PayStackResponse totalTransactions(int perPage, int page, String from, String to) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(totalTransactionUrl)
                .queryParam("perPage", perPage)
                .queryParam("page", page)
                .queryParam("from", from)
                .queryParam("to", to);

        return api.get(builder.toUriString(), PayStackResponse.class, getHeader());
    }

    private Map<String, String> getHeader() {
        Map<String, String> map = new HashMap<>();
        map.put("Content-type", "application/json");
        map.put("Authorization", "Bearer " + secretKey);
        return map;
    }
}
