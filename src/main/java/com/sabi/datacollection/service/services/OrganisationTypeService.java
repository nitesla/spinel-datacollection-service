package com.sabi.datacollection.service.services;

import com.sabi.datacollection.core.dto.request.EnableDisableDto;
import com.sabi.datacollection.core.dto.request.OrganisationTypeDto;
import com.sabi.datacollection.core.dto.response.OrganisationTypeResponseDto;
import com.sabi.datacollection.core.models.OrganisationType;
import com.sabi.datacollection.service.helper.Validations;
import com.sabi.datacollection.service.repositories.OrganisationTypeRepository;
import com.sabi.framework.exceptions.ConflictException;
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
public class OrganisationTypeService {

    private OrganisationTypeRepository organisationTypeRepository;
    private final ModelMapper mapper;
    private final Validations validations;


    public OrganisationTypeService(OrganisationTypeRepository organisationTypeRepository, ModelMapper mapper, Validations validations) {
        this.organisationTypeRepository = organisationTypeRepository;
        this.mapper = mapper;
        this.validations = validations;
    }

    public OrganisationTypeResponseDto createOrganisationType(OrganisationTypeDto request) {
        validations.validateOrganisationType(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        OrganisationType organisationType = mapper.map(request, OrganisationType.class);
        OrganisationType organisationTypeExist = organisationTypeRepository.findOrganisationTypeByName(request.getName());
        if(organisationTypeExist != null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Organisation Type already exist");
        }
        organisationType.setCreatedBy(userCurrent.getId());
        organisationType.setIsActive(true);
        organisationTypeRepository.save(organisationType);
        log.info("Created new Organisation type - {}", organisationType);
        return mapper.map(organisationType, OrganisationTypeResponseDto.class);
    }


    public OrganisationTypeResponseDto updateOrganisationType(OrganisationTypeDto request) {
        validations.validateOrganisationType(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        OrganisationType organisationType = organisationTypeRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Organisation Id does not exist!"));
        mapper.map(request, organisationType);
        organisationType.setCreatedBy(userCurrent.getId());
        organisationType.setIsActive(true);
        organisationTypeRepository.save(organisationType);
        log.info("Updated Organisation type - {}", organisationType);
        return mapper.map(organisationType, OrganisationTypeResponseDto.class);
    }

    public OrganisationTypeResponseDto findOrganisationTypeById(Long id) {
        OrganisationType organisationType = organisationTypeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Organisation Id does not exist!"));
        return mapper.map(organisationType, OrganisationTypeResponseDto.class);
    }

    public Page<OrganisationType> findAll(String name, String description, Pageable pageable) {
        Page<OrganisationType> organisationTypes = organisationTypeRepository
                .findOrganisationTypes(name, description, pageable);
        if (organisationTypes == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return organisationTypes;
    }

    public void enableDisableState (EnableDisableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        OrganisationType organisationType = organisationTypeRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Organisation Type Id does not exist!"));
        organisationType.setIsActive(request.getIsActive());
        organisationType.setUpdatedBy(userCurrent.getId());
        log.info("organisationType - {}", organisationType);
        organisationTypeRepository.save(organisationType);
    }

    public List<OrganisationType> getAll(Boolean isActive) {
        return organisationTypeRepository.findOrganisationTypeByIsActive(isActive);
    }
}
