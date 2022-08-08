package com.spinel.datacollection.service.services;


import com.spinel.datacollection.core.dto.request.EnableDisableDto;
import com.spinel.datacollection.core.dto.request.EnumeratorProjectLocationDto;
import com.spinel.datacollection.core.dto.response.EnumeratorProjectLocResponseDto;
import com.spinel.datacollection.core.models.EnumeratorProjectLocation;
import com.spinel.datacollection.service.helper.Validations;
import com.spinel.datacollection.service.repositories.EnumeratorProjectLocRepository;

import com.spinel.framework.exceptions.ConflictException;
import com.spinel.framework.exceptions.NotFoundException;
import com.spinel.framework.models.User;
import com.spinel.framework.service.TokenService;
import com.spinel.framework.utils.CustomResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@SuppressWarnings("ALL")
@Slf4j
@Service
public class EnumeratorProjectLocService {


    private final EnumeratorProjectLocRepository enumeratorProjectLocRepository;
    private final ModelMapper mapper;
    private final Validations validations;

    public EnumeratorProjectLocService(EnumeratorProjectLocRepository enumeratorProjectLocRepository, ModelMapper mapper, Validations validations) {
        this.enumeratorProjectLocRepository = enumeratorProjectLocRepository;
        this.mapper = mapper;
        this.validations = validations;
    }

    public EnumeratorProjectLocResponseDto createEnumeratorProjectLocation(EnumeratorProjectLocationDto request) {
        validations.validateEnumeratorProjectLocation(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        EnumeratorProjectLocation enumeratorProjectLocation = mapper.map(request, EnumeratorProjectLocation.class);
        EnumeratorProjectLocation enumeratorProjectLocationExist = enumeratorProjectLocRepository.findByEnumeratorProjectIdAndProjectLocationId(request.getEnumeratorProjectId(), request.getProjectLocationId());
        if(enumeratorProjectLocationExist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Enumerator Project Location already exist");
        }
        enumeratorProjectLocation.setCreatedBy(userCurrent.getId());
        enumeratorProjectLocation.setIsActive(true);
        enumeratorProjectLocRepository.save(enumeratorProjectLocation);
        log.info("Created new Enumerator Project Location - {}", enumeratorProjectLocation);
        return mapper.map(enumeratorProjectLocation, EnumeratorProjectLocResponseDto.class);
    }

    public EnumeratorProjectLocResponseDto updateEnumeratorProjectLocation(EnumeratorProjectLocationDto request) {
        validations.validateEnumeratorProjectLocation(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        EnumeratorProjectLocation enumeratorProjectLocation = enumeratorProjectLocRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Enumerator Project Location Id does not exist!"));
        mapper.map(request, enumeratorProjectLocation);
        enumeratorProjectLocation.setUpdatedBy(userCurrent.getId());
        enumeratorProjectLocRepository.save(enumeratorProjectLocation);
        log.info("Enumerator Project Location record updated - {}", enumeratorProjectLocation);
        return mapper.map(enumeratorProjectLocation, EnumeratorProjectLocResponseDto.class);
    }

    public EnumeratorProjectLocResponseDto findEnumeratorProjectLocationById(Long id){
        EnumeratorProjectLocation enumeratorProjectLocation = enumeratorProjectLocRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Enumerator Project Location Id does not exist!"));
        return mapper.map(enumeratorProjectLocation, EnumeratorProjectLocResponseDto.class);
    }


    public Page<EnumeratorProjectLocation> findAll(Long enumeratorProjectId, Long projectLocationId, Integer collectedRecord, Integer expectedRecord, PageRequest pageRequest ) {
        Page<EnumeratorProjectLocation> projectCategories = enumeratorProjectLocRepository.findEnumeratorProjectLocations(enumeratorProjectId, projectLocationId, collectedRecord, expectedRecord, pageRequest);
        if (projectCategories == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return projectCategories;

    }

    public void enableDisableState (EnableDisableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        EnumeratorProjectLocation enumeratorProjectLocation = enumeratorProjectLocRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Enumerator Project Location Id does not exist!"));
        enumeratorProjectLocation.setIsActive(request.getIsActive());
        enumeratorProjectLocation.setUpdatedBy(userCurrent.getId());
        enumeratorProjectLocRepository.save(enumeratorProjectLocation);

    }

    public List<EnumeratorProjectLocation> getAll(Boolean isActive){
        return enumeratorProjectLocRepository.findByIsActive(isActive);
    }
}
