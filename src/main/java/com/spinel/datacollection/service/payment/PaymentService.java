package com.spinel.datacollection.service.payment;

import com.spinel.datacollection.core.dto.payment.request.InitializeTransactionRequest;
import com.spinel.datacollection.core.dto.payment.request.VerifyTransaction;
import com.spinel.datacollection.core.dto.payment.response.InitializeTransactionResponse;
import com.spinel.datacollection.core.dto.payment.response.VerifyTransactionResponse;

public interface PaymentService {
    InitializeTransactionResponse initializeTransaction(InitializeTransactionRequest request);
    VerifyTransactionResponse verifyTransaction(VerifyTransaction verifyTransaction);
}
