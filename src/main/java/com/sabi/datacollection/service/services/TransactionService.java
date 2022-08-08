package com.sabi.datacollection.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sabi.datacollection.core.dto.request.EnableDisableDto;
import com.sabi.datacollection.core.dto.request.TransactionDto;
import com.sabi.datacollection.core.dto.response.TransactionResponseDto;
import com.sabi.datacollection.core.enums.ActionType;
import com.sabi.datacollection.core.enums.Status;
import com.sabi.datacollection.core.enums.TransactionType;
import com.sabi.datacollection.core.models.Transaction;
import com.sabi.datacollection.service.helper.Validations;
import com.sabi.datacollection.service.repositories.TransactionRepository;
import com.sabi.framework.exceptions.BadRequestException;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
        Transaction transactionExist = transactionRepository.findByHash(request.getHash());
        if(transactionExist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Transaction already exist");
        }
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
        if (request.getId() == null)
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION,"The Id cannot be null");
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
    public Page<Transaction> findAll(Long walletId,
                                     BigDecimal amount,
                                     BigDecimal initialBalance,
                                     BigDecimal finalBalance,
                                     ActionType actionType,
                                     TransactionType transactionType,
                                     Status status,
                                     String reference,
                                     LocalDateTime fromDate,
                                     LocalDateTime toDate,
                                     PageRequest pageRequest ){
        if (fromDate!=null){
            if (toDate!=null && fromDate.isAfter(toDate))
                throw new BadRequestException(CustomResponseCode.BAD_REQUEST,"fromDate can't be greater than toDate");
             }
        if (toDate!=null){
            if (fromDate == null)
                throw new BadRequestException(CustomResponseCode.BAD_REQUEST,"'fromDate' must be included along with 'toDate' in the request");
        }
        Page<Transaction> transaction = transactionRepository.findTransactions(walletId,
                amount, initialBalance,finalBalance,actionType,transactionType,status, reference,fromDate,toDate, pageRequest);
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
