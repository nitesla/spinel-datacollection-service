package com.spinel.datacollection.service.payment;

import com.spinel.datacollection.core.dto.payment.request.*;
import com.spinel.datacollection.core.dto.payment.response.InitializeTransactionResponse;
import com.spinel.datacollection.core.dto.payment.response.TotalTransactionResponse;
import com.spinel.datacollection.core.dto.payment.response.TransactionResponse;

import java.util.List;

public interface PaymentService {
    InitializeTransactionResponse initializeTransaction(InitializeTransaction initializeTransaction);
    TransactionResponse verifyTransaction(VerifyTransaction verifyTransaction);
    TotalTransactionResponse totalTransactions(TotalTransaction totalTransaction);
    List<TransactionResponse> listTransactions(ListTransactions listTransactions);
    TransactionResponse fetchTransaction(String transactionId);


}
