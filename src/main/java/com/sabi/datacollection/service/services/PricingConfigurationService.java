package com.sabi.datacollection.service.services;

import com.sabi.datacollection.core.dto.request.EnableDisableDto;
import com.sabi.datacollection.core.dto.request.PricingConfigurationDto;
import com.sabi.datacollection.core.dto.response.PricingConfigurationResponseDto;
import com.sabi.datacollection.core.models.PricingConfiguration;
import com.sabi.datacollection.core.models.ProjectLocation;
import com.sabi.datacollection.service.helper.Validations;
import com.sabi.datacollection.service.repositories.PricingConfigurationRepository;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
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

    private final PricingConfigurationRepository pricingConfigurationRepository;
    private final ModelMapper mapper;
    private final Validations validations;


    public PricingConfigurationService(PricingConfigurationRepository pricingConfigurationRepository, ModelMapper mapper, Validations validations) {
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
        log.info("Updated Pricing Configuration", pricingConfiguration);
        return mapper.map(pricingConfiguration, PricingConfigurationResponseDto.class);
    }
    
    public PricingConfigurationResponseDto findPricingConfiguration(Long id) {
        PricingConfiguration pricingConfiguration = pricingConfigurationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Enter a valid Pricing Configuration Id"));
        return mapper.map(pricingConfiguration, PricingConfigurationResponseDto.class);
    }

    public Page<PricingConfiguration> findAll(Pageable pageable) {
        Page<PricingConfiguration> pricingConfigurations = pricingConfigurationRepository.findAll(pageable);
        if (pricingConfigurations == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, "No record found");
        }
        return pricingConfigurations;
    }

    public Page<PricingConfiguration> findPricingConfigurationByDataSetId(Long dataSetId, Pageable pageable) {
        Page<PricingConfiguration> pricingConfigurations = pricingConfigurationRepository.findPricingConfigurationByDataSetId(dataSetId, pageable);
        if (pricingConfigurations == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, "No record found");
        }
        return pricingConfigurations;
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
        return pricingConfigurationRepository.findPricingConfigurationByIsActive(isActive);
    }
}
