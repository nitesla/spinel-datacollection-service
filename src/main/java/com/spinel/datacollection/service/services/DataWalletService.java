package com.spinel.datacollection.service.services;


import com.spinel.datacollection.core.dto.request.EnableDisableDto;
import com.spinel.datacollection.core.dto.wallet.CreateWalletDto;
import com.spinel.datacollection.core.dto.wallet.WalletResponseDto;
import com.spinel.datacollection.core.models.Wallet;
import com.spinel.datacollection.service.repositories.DataWalletRepository;
import com.spinel.framework.exceptions.NotFoundException;
import com.spinel.framework.models.User;
import com.spinel.framework.service.TokenService;
import com.spinel.framework.utils.CustomResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class DataWalletService {

    private final DataWalletRepository dataWalletRepository;
    private final ModelMapper mapper;

    public DataWalletService(DataWalletRepository dataWalletRepository, ModelMapper mapper) {
        this.dataWalletRepository = dataWalletRepository;
        this.mapper = mapper;
    }

    public WalletResponseDto createWallet(CreateWalletDto request) {
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Wallet wallet = mapper.map(request, Wallet.class);
        wallet.setIsActive(true);
        wallet.setCreatedDate(LocalDateTime.now());
        wallet.setUserId(userCurrent.getId());
        wallet = dataWalletRepository.save(wallet);
        log.info("Created wallet - {}", wallet);
        return mapper.map(wallet, WalletResponseDto.class);
    }

    public WalletResponseDto findWalletById(Long id) {
        Wallet wallet = dataWalletRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Wallet Id does not exist"));
        return mapper.map(wallet, WalletResponseDto.class);
    }

    public WalletResponseDto findWalletByLastTransactionId(Long lastTransactionId) {
        Wallet wallet = dataWalletRepository.findByLastTransactionId(lastTransactionId);
        if(wallet == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                    "Invalid Transaction Id, try again");
        }
        return mapper.map(wallet, WalletResponseDto.class);
    }

    public WalletResponseDto findWalletByUserId(Long userId) {
        Wallet wallet = dataWalletRepository.findByUserId(userId);
        if(wallet == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                    "Invalid User Id, try again");
        }
        return mapper.map(wallet, WalletResponseDto.class);
    }

    public WalletResponseDto findWalletByIdentificationNumber(String identificationNumber) {
        Wallet wallet = dataWalletRepository.findByIdentificationNumber(identificationNumber);
        if(wallet == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                    "Invalid identification Number, try again");
        }
        return mapper.map(wallet, WalletResponseDto.class);
    }

    public Page<Wallet> findAll(Pageable pageable) {
        return dataWalletRepository.findAll(pageable);
    }


    public void enableDisableState (EnableDisableDto request){
        Wallet wallet = dataWalletRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Wallet Id does not exist!"));
        wallet.setIsActive(request.getIsActive());
        dataWalletRepository.save(wallet);
    }

    public List<Wallet> getAll(Boolean isActive) {
        return dataWalletRepository.findByIsActive(isActive);
    }
}
