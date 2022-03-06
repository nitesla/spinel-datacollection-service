package com.sabi.datacollection.service.services;

import com.sabi.datacollection.core.dto.request.IndicatorDictionaryDto;
import com.sabi.datacollection.core.dto.request.EnableDisableDto;
import com.sabi.datacollection.core.dto.response.IndicatorDictionaryResponseDto;
import com.sabi.datacollection.core.models.IndicatorDictionary;
import com.sabi.datacollection.service.helper.Validations;
import com.sabi.datacollection.service.repositories.IndicatorDictionaryRepository;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@SuppressWarnings("ALL")
@Slf4j
@Service
public class IndicatorDictionaryService {

    private final IndicatorDictionaryRepository indicatorDictionaryRepository;
    private final ModelMapper mapper;
    private final Validations validations;

    public IndicatorDictionaryService(IndicatorDictionaryRepository indicatorDictionaryRepository, ModelMapper mapper, Validations validations) {
        this.indicatorDictionaryRepository = indicatorDictionaryRepository;
        this.mapper = mapper;
        this.validations = validations;
    }

    public IndicatorDictionaryResponseDto createIndicatorDictionary (IndicatorDictionaryDto request) {
        validations.validateIndicatorDictionary(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        IndicatorDictionary indicatorDictionary = mapper.map(request, IndicatorDictionary.class);
        IndicatorDictionary indicatorDictionaryExist = indicatorDictionaryRepository.findByName(request.getName());
        if(indicatorDictionaryExist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "Indicator Dictionary already exist");
        }
        indicatorDictionary.setCreatedBy(userCurrent.getId());
        indicatorDictionary.setIsActive(true);
        indicatorDictionaryRepository.save(indicatorDictionary);
        log.info("Created new Indicator Dictionary - {}", indicatorDictionary);
        return mapper.map(indicatorDictionary, IndicatorDictionaryResponseDto.class);
    }

    public IndicatorDictionaryResponseDto updateIndicatorDictionary(IndicatorDictionaryDto request) {
        validations.validateIndicatorDictionary(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        IndicatorDictionary indicatorDictionary = indicatorDictionaryRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Indicator Dictionary Id does not exist!"));
        mapper.map(request, indicatorDictionary);
        indicatorDictionary.setUpdatedBy(userCurrent.getId());
        indicatorDictionaryRepository.save(indicatorDictionary);
        log.info("Indicator Dictionary record updated - {}", indicatorDictionary);
        return mapper.map(indicatorDictionary, IndicatorDictionaryResponseDto.class);
    }

    public IndicatorDictionaryResponseDto findIndicatorDictionaryById(Long id){
        IndicatorDictionary indicatorDictionary = indicatorDictionaryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Indicator Dictionary Id does not exist!"));
        return mapper.map(indicatorDictionary, IndicatorDictionaryResponseDto.class);
    }


    public Page<IndicatorDictionary> findAll(String name, PageRequest pageRequest ) {
        Page<IndicatorDictionary> indicatorDictionaries = indicatorDictionaryRepository.findIndicatorDictionaries(name, pageRequest);
        if (indicatorDictionaries == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, "No record found !");
        }
        return indicatorDictionaries;

    }

    public void enableDisableState (EnableDisableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        IndicatorDictionary indicatorDictionary = indicatorDictionaryRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Indicator Dictionary Id does not exist!"));
        indicatorDictionary.setIsActive(request.getIsActive());
        indicatorDictionary.setUpdatedBy(userCurrent.getId());
        indicatorDictionaryRepository.save(indicatorDictionary);

    }

    public List<IndicatorDictionary> getAll(Boolean isActive){
        return indicatorDictionaryRepository.findByIsActive(isActive);
    }

}
