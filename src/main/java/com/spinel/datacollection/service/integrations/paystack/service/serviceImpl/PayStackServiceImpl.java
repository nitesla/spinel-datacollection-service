package com.spinel.datacollection.service.integrations.paystack.service.serviceImpl;


import com.spinel.datacollection.core.dto.payment.request.*;
import com.spinel.datacollection.core.dto.payment.response.*;
import com.spinel.datacollection.core.integrations.paystack.dto.response.*;
import com.spinel.datacollection.core.integrations.paystack.dto.response.chargeauthorization.PayStackChargeAuthorizationResponse;
import com.spinel.datacollection.core.integrations.paystack.dto.response.resolvecardbin.PayStackResolveCardBinResponse;
import com.spinel.datacollection.core.integrations.paystack.dto.response.singletransfer.PayStackSingleTransferResponse;
import com.spinel.datacollection.core.integrations.paystack.dto.response.subscription.PayStackCreateSubscriptionPlanResponse;
import com.spinel.datacollection.core.integrations.paystack.dto.response.transferrecipient.PaystackTransferRecipientResponse;
import com.spinel.datacollection.service.integrations.paystack.enums.PayStackCurrency;
import com.spinel.datacollection.service.integrations.paystack.enums.PayStackTransactionStatus;
import com.spinel.datacollection.service.integrations.paystack.enums.SubscriptionPlanInterval;
import com.spinel.datacollection.service.payment.PaymentService;
import com.spinel.framework.exceptions.BadRequestException;
import com.spinel.framework.helpers.API;
import com.spinel.framework.utils.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Slf4j
@RequiredArgsConstructor
@Service
public class PayStackServiceImpl implements PaymentService {

    @Value("${paystack.secret.key}")
    private String secretKey;

//    @Value("${paystack.initialize.transaction.url}")
//    private String initializeTransactionUrl;
//
//    @Value("${paystack.verify.transaction.url}")
//    private String verifyTransactionUrl;
//
//    @Value("${paystack.list.transaction.url}")
//    private String listTransactionUrl;
//
//    @Value("${paystack.fetch.transaction.url}")
//    private String fetchTransactionUrl;
//
//    @Value("${paystack.total.transaction.url}")
//    private String totalTransactionUrl;

    @Value("${paystack.base.url}")
    private String baseUrl;

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



        PayStackResponse response = api.post(baseUrl + "transaction/initialize", requestBody, PayStackResponse.class, getHeader());
        PayStackInitializeTransactionDataResponse dataResponse = mapper.map(response.getData(), PayStackInitializeTransactionDataResponse.class);
        InitializeTransactionResponse initializeTransactionResponse = new InitializeTransactionResponse();
        initializeTransactionResponse.setReference(dataResponse.getReference());
        initializeTransactionResponse.setMessage(response.getMessage());
        initializeTransactionResponse.setStatus(response.isStatus());
        initializeTransactionResponse.setAccessCode(dataResponse.getAccess_code());
        initializeTransactionResponse.setUrl(dataResponse.getAuthorization_url());
        return initializeTransactionResponse;
    }

    @Override
    public TransactionResponse verifyTransaction(VerifyTransaction verifyTransaction) {
        String url = baseUrl + "transaction/verify/" + verifyTransaction.getReference();
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
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl + "transaction")
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
        String url = baseUrl + "transaction" + Integer.parseInt(transactionId);
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
    public ValidateCustomerResponse validateCustomer(ValidateCustomer validateCustomer) {
        String url = baseUrl + "customer/" + validateCustomer.getCustomerCode() + "/identification";

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("country", validateCustomer.getCountry());
        requestBody.put("type", validateCustomer.getType());
        requestBody.put("account_number", validateCustomer.getAccountNumber());
        requestBody.put("bvn", validateCustomer.getBvn());
        requestBody.put("bank_code", validateCustomer.getBankCode());
        requestBody.put("first_name", validateCustomer.getFirstName());
        requestBody.put("last_name", validateCustomer.getLastName());

        PayStackResponse response =  api.post(url, requestBody, PayStackResponse.class, getHeader());

        return ValidateCustomerResponse.builder()
                .status(response.isStatus())
                .message(response.getMessage())
                .build();
    }

    @Override
    public ResolveAccountNumberResponse resolveAccountNumber(ResolveAccountNumber resolveAccountNumber) {
        PayStackResolveAccountNumberResponse response = resolveAccountNumber(
                resolveAccountNumber.getAccountNumber(), resolveAccountNumber.getBankCode());

        return ResolveAccountNumberResponse.builder()
                .message(response.getMessage())
                .status(response.isStatus())
                .accountName(response.getData().getAccount_name())
                .accountNumber(response.getData().getAccount_number())
                .bankId(String.valueOf(response.getData().getBank_id()))
                .build();
    }

    @Override
    public SingleTransferResponse singleTransfer(SingleTransfer singleTransfer) {
        //resolve account number
        PayStackResolveAccountNumberResponse resolveAccountNumberResponse = resolveAccountNumber(
                singleTransfer.getAccountNumber(), singleTransfer.getBankCode()
        );

        //create a transfer recipient
        PaystackTransferRecipientResponse transferRecipientResponse = createTransferRecipient(
                singleTransfer.getType(),
                resolveAccountNumberResponse.getData().getAccount_name(),
                resolveAccountNumberResponse.getData().getAccount_number(),
                singleTransfer.getBankCode(),
                singleTransfer.getCurrency()
        );

        //initiate a transfer
        PayStackSingleTransferResponse response = singleTransfer(
                singleTransfer.getSource(),
                singleTransfer.getAmount(),
                transferRecipientResponse.getData().getRecipient_code(),
                singleTransfer.getDescription()
        );

        return SingleTransferResponse.builder()
                .message(response.getMessage())
                .reference(response.getData().getReference())
                .domain(response.getData().getDomain())
                .currency(response.getData().getCurrency())
                .amount(response.getData().getAmount())
                .source(response.getData().getSource())
                .description(response.getData().getReason())
                .status(response.getData().getStatus())
                .transferCode(response.getData().getTransfer_code())
                .id(response.getData().getId())
                .recipient(response.getData().getRecipient())
                .createdAt(response.getData().getCreatedAt())
                .updatedAt(response.getData().getUpdatedAt())
                .build();


        //listen for status
    }

    private PayStackResolveAccountNumberResponse resolveAccountNumber(String accountNumber, String bankCode) {
        String url = baseUrl + "bank/resolve";
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url)
                    .queryParam("account_number", accountNumber)
                    .queryParam("bank_code", bankCode);
            return api.get(builder.toUriString(), PayStackResolveAccountNumberResponse.class, getHeader());
        } catch (Exception e) {
            log.error("Error occurred while resolving account number: {}", e.getMessage());
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Error occurred while resolving account number");
        }
    }

    private PaystackTransferRecipientResponse createTransferRecipient(String type, String name, String accountNumber, String bankCode, String currency) {
        String url = baseUrl + "transferrecipient";
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("type", type);
        requestBody.put("name", name);
        requestBody.put("account_number", accountNumber);
        requestBody.put("bank_code", bankCode);
        requestBody.put("currency", currency);
        try {
            return api.post(url, requestBody, PaystackTransferRecipientResponse.class, getHeader());
        } catch (Exception e) {
            log.error("Error occurred while creating transfer recipient: {}", e.getMessage());
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Error occurred while creating transfer recipient");
        }
    }

    private PayStackSingleTransferResponse singleTransfer(String source, BigDecimal amount, String recipient, String reason) {
        String url = baseUrl + "transfer";
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("source", source);
        requestBody.put("amount", amount.toString());
        requestBody.put("recipient", recipient);
        requestBody.put("reason", reason);
        try {
            return api.post(url, requestBody, PayStackSingleTransferResponse.class, getHeader());
        } catch (Exception e) {
            log.error("Error occurred during transfer: {}", e.getMessage());
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Error occurred during transfer");
        }
    }



    @Override
    public CreateSubscriptionResponse createSubscription(CreateSubscription request) {
        //create a plan
        PayStackCreateSubscriptionPlanResponse createPlan = createAPlan(
                request.getName(),
                request.getInterval(),
                request.getAmount()
        );

        //create a subscription
        PayStackInitializeTransactionResponse response = createSubscription(
                request.getEmail(),
                createPlan.getData().getAmount(),
                createPlan.getData().getPlan_code()
        );

        return CreateSubscriptionResponse.builder()
                .reference(response.getData().getReference())
                .status(Boolean.parseBoolean(response.getStatus()))
                .accessCode(response.getData().getAccess_code())
                .url(response.getData().getAuthorization_url())
                .message(response.getMessage())
                .amount(createPlan.getData().getAmount())
                .build();

        //listen for status
    }

    @Override
    public ResolveCardBinResponse resolveCardBin(ResolveCardBin resolveCardBin) {
        String url = baseUrl + "decision/bin/" + resolveCardBin.getPinDigits();

        PayStackResolveCardBinResponse response =
                api.get(url, PayStackResolveCardBinResponse.class, getHeader());

        return ResolveCardBinResponse.builder()
                .status(response.isStatus())
                .message(response.getMessage())
                .bin(response.getData().getBin())
                .brand(response.getData().getBrand())
                .subBrand(response.getData().getSub_brand())
                .countryCode(response.getData().getCountry_code())
                .countryName(response.getData().getCountry_name())
                .cardType(response.getData().getCard_type())
                .bank(response.getData().getBank())
                .linkedBankId(response.getData().getLinked_bank_id())
                .build();
    }

    @Override
    public ChargeAuthorizationResponse chargeAuthorization(ChargeAuthorization chargeAuthorization) {
        String url = baseUrl + "transaction/charge_authorization";

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("authorization_code", chargeAuthorization.getAuthorizationCode());
        requestBody.put("email", chargeAuthorization.getEmail());
        requestBody.put("amount", chargeAuthorization.getAmount().toString());

        PayStackChargeAuthorizationResponse response =
                api.post(url, requestBody, PayStackChargeAuthorizationResponse.class, getHeader());

        return ChargeAuthorizationResponse.builder()
                .status(response.isStatus())
                .message(response.getMessage())
                .amount(response.getData().getAmount())
                .transactionDate(response.getData().getTransaction_date())
                .reference(response.getData().getReference())
                .gatewayResponse(response.getData().getGateway_response())
                .authorizationCode(response.getData().getAuthorization().getAuthorization_code())
                .bank(response.getData().getAuthorization().getBank())
                .build();
    }

    private PayStackCreateSubscriptionPlanResponse createAPlan(String name, String interval, BigDecimal amount) {
        String url = baseUrl + "plan";
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("name", name);
        requestBody.put("interval", SubscriptionPlanInterval.isValidPayStackInterval(interval).getAlias());
        requestBody.put("amount", amount.toString());

        try {
            return api.post(url, requestBody, PayStackCreateSubscriptionPlanResponse.class, getHeader());
        } catch (Exception e) {
            log.error("Error occurred while creating subscription plan: {}", e.getMessage());
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Error occurred while creating subscription plan");
        }
    }

    private PayStackInitializeTransactionResponse createSubscription(String email, BigDecimal amount, String plan) {
        String url = baseUrl + "transaction/initialize";

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("email", email);
        requestBody.put("plan", plan);
        requestBody.put("amount", amount.toString());

        try {
            return api.post(url, requestBody, PayStackInitializeTransactionResponse.class, getHeader());
        } catch (Exception e) {
            log.error("Error occurred while creating subscription: {}", e.getMessage());
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Error occurred while creating subscription");
        }
    }


    @Override
    public TotalTransactionResponse totalTransactions(TotalTransaction totalTransaction) {
        String url = baseUrl + "transaction/totals";
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url)
                .queryParam("perPage", totalTransaction.getPerPage())
                .queryParam("page", totalTransaction.getPage())
                .queryParam("from", totalTransaction.getFrom())
                .queryParam("to", totalTransaction.getTo());

        PayStackTotalTransactionResponse totalTransactionResponse = api.get(builder.toUriString(), PayStackTotalTransactionResponse.class, getHeader());
        return TotalTransactionResponse.builder()
                .totalTransaction(totalTransactionResponse.getData().getTotal_transaction())
                .totalVolume(totalTransactionResponse.getData().getTotal_volume())
                .pendingTransfers(totalTransactionResponse.getData().getPending_transfers())
                .build();
    }

    private Map<String, String> getHeader() {
        Map<String, String> map = new HashMap<>();
        map.put("Content-type", "application/json");
        map.put("Authorization", "Bearer " + secretKey);
        return map;
    }
}
