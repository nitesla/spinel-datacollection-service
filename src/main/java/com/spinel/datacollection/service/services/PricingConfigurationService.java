package com.spinel.datacollection.service.services;


import com.spinel.datacollection.core.dto.request.EnableDisableDto;
import com.spinel.datacollection.core.dto.request.PricingConfigurationDto;
import com.spinel.datacollection.core.dto.response.PricingConfigurationResponseDto;
import com.spinel.datacollection.core.models.DataSet;
import com.spinel.datacollection.core.models.PricingConfiguration;
import com.spinel.datacollection.service.helper.Validations;
import com.spinel.datacollection.service.repositories.DataSetRepository;
import com.spinel.datacollection.service.repositories.PricingConfigurationRepository;

import com.spinel.framework.exceptions.NotFoundException;
import com.spinel.framework.models.User;
import com.spinel.framework.service.TokenService;
import com.spinel.framework.utils.CustomResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@SuppressWarnings("ALL")
@Slf4j
@Service
public class PricingConfigurationService {

    private final DataSetRepository dataSetRepository;
    private final PricingConfigurationRepository pricingConfigurationRepository;
    private final ModelMapper mapper;
    private final Validations validations;


    public PricingConfigurationService(DataSetRepository dataSetRepository, PricingConfigurationRepository pricingConfigurationRepository, ModelMapper mapper, Validations validations) {
        this.dataSetRepository = dataSetRepository;
        this.pricingConfigurationRepository = pricingConfigurationRepository;
        this.mapper = mapper;
        this.validations = validations;
    }

    public PricingConfigurationResponseDto createPricingConfiguration(PricingConfigurationDto request) {
        validations.validatePricingConfiguration(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PricingConfiguration pricingConfiguration = mapper.map(request, PricingConfiguration.class);
        pricingConfiguration.setCreatedBy(userCurrent.getId());
        pricingConfiguration.setCreatedBy(userCurrent.getId());
        pricingConfiguration.setIsActive(true);
        pricingConfigurationRepository.save(pricingConfiguration);
        addDataSet(pricingConfiguration);
        log.info("Created new Pricing Configuration", pricingConfiguration);
        return mapper.map(pricingConfiguration, PricingConfigurationResponseDto.class);
    }

    public PricingConfigurationResponseDto updatePricingConfiguration(PricingConfigurationDto request) {
        validations.validatePricingConfiguration(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PricingConfiguration pricingConfiguration = pricingConfigurationRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Enter a valid Pricing Configuration Id"));
        mapper.map(request, pricingConfiguration);
        pricingConfiguration.setCreatedBy(userCurrent.getId());
        pricingConfigurationRepository.save(pricingConfiguration);
        addDataSet(pricingConfiguration);
        log.info("Updated Pricing Configuration", pricingConfiguration);
        return mapper.map(pricingConfiguration, PricingConfigurationResponseDto.class);
    }
    
    public PricingConfigurationResponseDto findPricingConfiguration(Long id) {
        PricingConfiguration pricingConfiguration = pricingConfigurationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Enter a valid Pricing Configuration Id"));
        addDataSet(pricingConfiguration);
        return mapper.map(pricingConfiguration, PricingConfigurationResponseDto.class);
    }

    public Page<PricingConfiguration> findAll(Pageable pageable) {
        Page<PricingConfiguration> pricingConfigurationPage = pricingConfigurationRepository.findAll(pageable);
        if (pricingConfigurationPage == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, "No record found");
        }
        pricingConfigurationPage.getContent().forEach(pricingConfiguration -> {
            addDataSet(pricingConfiguration);
        });
        return pricingConfigurationPage;
    }

    public Page<PricingConfiguration> findPricingConfigurationByDataSetId(Long dataSetId, Pageable pageable) {
        Page<PricingConfiguration> pricingConfigurationPage = pricingConfigurationRepository.findPricingConfigurationByDataSetId(dataSetId, pageable);
        if (pricingConfigurationPage == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, "No record found");
        }
        pricingConfigurationPage.getContent().forEach(pricingConfiguration -> {
            addDataSet(pricingConfiguration);
        });
        return pricingConfigurationPage;
    }

    public void enableDisableState (EnableDisableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        PricingConfiguration pricingConfiguration = pricingConfigurationRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Pricing configuration Id does not exist!"));
        pricingConfiguration.setIsActive(request.getIsActive());
        pricingConfiguration.setUpdatedBy(userCurrent.getId());
        pricingConfigurationRepository.save(pricingConfiguration);
    }

    public List<PricingConfiguration> getAll(Boolean isActive) {
        List<PricingConfiguration> pricingConfigurations = pricingConfigurationRepository.findPricingConfigurationByIsActive(isActive);
        pricingConfigurations.forEach(pricingConfiguration -> {
            addDataSet(pricingConfiguration);
        });
        return pricingConfigurations;
    }

    private void addDataSet(PricingConfiguration pricingConfiguration) {
        if (pricingConfiguration.getDataSetId() != null) {
            DataSet dataSet = dataSetRepository.findById(pricingConfiguration.getDataSetId())
                    .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                            "Request DataSet Id does not exist"));
            pricingConfiguration.setDataSet(dataSet.getName());
        }
    }
}
