package com.spinel.datacollection.service.payment;

import com.spinel.datacollection.core.dto.payment.request.*;
import com.spinel.datacollection.core.dto.payment.response.*;

import java.util.List;

public interface PaymentService {
    InitializeTransactionResponse initializeTransaction(InitializeTransaction initializeTransaction);
    TransactionResponse verifyTransaction(VerifyTransaction verifyTransaction);
    TotalTransactionResponse totalTransactions(TotalTransaction totalTransaction);
    List<TransactionResponse> listTransactions(ListTransactions listTransactions);
    TransactionResponse fetchTransaction(String transactionId);
    ValidateCustomerResponse validateCustomer(ValidateCustomer validateCustomer);
    ResolveAccountNumberResponse resolveAccountNumber(ResolveAccountNumber resolveAccountNumber);
    SingleTransferResponse singleTransfer(SingleTransfer singleTransfer);
    CreateSubscriptionResponse createSubscription(CreateSubscription createSubscription);
    ResolveCardBinResponse resolveCardBin(ResolveCardBin resolveCardBin);
    ChargeAuthorizationResponse chargeAuthorization(ChargeAuthorization chargeAuthorization);


}
