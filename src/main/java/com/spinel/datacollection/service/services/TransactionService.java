package com.spinel.datacollection.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.spinel.datacollection.core.dto.request.EnableDisableDto;
import com.spinel.datacollection.core.dto.request.TransactionDto;
import com.spinel.datacollection.core.dto.response.TransactionResponseDto;
import com.spinel.datacollection.core.models.Transaction;
import com.spinel.datacollection.service.helper.Validations;
import com.spinel.datacollection.service.repositories.TransactionRepository;

import com.spinel.framework.exceptions.NotFoundException;
import com.spinel.framework.models.User;
import com.spinel.framework.service.TokenService;
import com.spinel.framework.utils.CustomResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;


/**
 *
 * This class is responsible for all business logic for transaction
 */


@Slf4j
@Service
public class TransactionService {


    @Autowired
    private TransactionRepository transactionRepository;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;
    private final Validations validations;

    public TransactionService(TransactionRepository transactionRepository, ModelMapper mapper, ObjectMapper objectMapper, Validations validations) {
        this.transactionRepository = transactionRepository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.validations = validations;

    }

    /** <summary>
      * Transaction creation
      * </summary>
      * <remarks>this method is responsible for creation of new transactions</remarks>
      */

    public TransactionResponseDto createTransaction(TransactionDto request) {
        validations.validateTransaction(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Transaction transaction = mapper.map(request,Transaction.class);
//        Transaction transactionExist = transactionRepository.findByName(request.getName());
//        if(transactionExist !=null){
//            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Transaction already exist");
//        }
        transaction.setReference(validations.generateReferenceNumber(10));
        transaction.setCreatedBy(userCurrent.getId());
        transaction.setIsActive(true);
        transaction = transactionRepository.save(transaction);
        log.debug("Create new Transaction - {}"+ new Gson().toJson(transaction));
        return mapper.map(transaction, TransactionResponseDto.class);
    }


    /** <summary>
     * Transaction update
     * </summary>
     * <remarks>this method is responsible for updating already existing transactions</remarks>
     */

    public TransactionResponseDto updateTransaction(TransactionDto request) {
        validations.validateTransaction(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Transaction transaction = transactionRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Transaction Id does not exist!"));
        mapper.map(request, transaction);
        transaction.setUpdatedBy(userCurrent.getId());
        transactionRepository.save(transaction);
        log.debug("Transaction record updated - {}"+ new Gson().toJson(transaction));
        return mapper.map(transaction, TransactionResponseDto.class);
    }


    /** <summary>
     * Find Transaction
     * </summary>
     * <remarks>this method is responsible for getting a single record</remarks>
     */
    public TransactionResponseDto findTransaction(Long id){
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Transaction Id does not exist!"));

        return mapper.map(transaction,TransactionResponseDto.class);
    }


    /** <summary>
     * Find all Transaction
     * </summary>
     * <remarks>this method is responsible for getting all records in pagination</remarks>
     */
    public Page<Transaction> findAll(Long walletId, BigDecimal amount, String reference,  PageRequest pageRequest ){
        Page<Transaction> transaction = transactionRepository.findTransactions(walletId, amount, reference, pageRequest);
            if(transaction == null){
                throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
            }

            return transaction;
    }


    /** <summary>
     * Enable disenable
     * </summary>
     * <remarks>this method is responsible for enabling and disabling a transaction</remarks>
     */
    public void enableDisEnableTransaction (EnableDisableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Transaction transaction = transactionRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Transaction Id does not exist!"));
        transaction.setIsActive(request.getIsActive());
        transaction.setUpdatedBy(userCurrent.getId());
        transactionRepository.save(transaction);

    }

    public List<Transaction> getAll(Boolean isActive){
        List<Transaction> transactions = transactionRepository.findByIsActive(isActive);
        return transactions;

    }


}
