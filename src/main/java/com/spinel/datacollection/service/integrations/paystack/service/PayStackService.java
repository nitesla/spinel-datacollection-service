package com.spinel.datacollection.service.integrations.paystack.service;

import com.spinel.datacollection.core.integrations.paystack.dto.request.InitializeTransaction;
import com.spinel.datacollection.core.integrations.paystack.dto.response.PayStackResponse;

public interface PayStackService {
    PayStackResponse initializeTransaction(InitializeTransaction initializeTransaction);
    PayStackResponse verifyTransaction(String reference);
    PayStackResponse listTransaction(int perPage, int page, String from, String to, String status, String customer);
    PayStackResponse fetchTransaction(String transactionId);
    PayStackResponse totalTransactions(int perPage, int page, String from, String to);
}
