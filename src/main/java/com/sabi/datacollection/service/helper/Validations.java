package com.sabi.datacollection.service.helper;


import com.sabi.datacollection.core.dto.request.*;
import com.sabi.datacollection.core.enums.Status;
import com.sabi.datacollection.core.models.Country;
import com.sabi.datacollection.core.models.LGA;
import com.sabi.datacollection.core.models.State;
import com.sabi.datacollection.service.repositories.*;
import com.sabi.framework.exceptions.BadRequestException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.repositories.RoleRepository;
import com.sabi.framework.repositories.UserRepository;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.framework.utils.Utility;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.stereotype.Service;

import java.util.Base64;

@SuppressWarnings("All")
@Slf4j
@Service
public class Validations {

    private RoleRepository roleRepository;
    private CountryRepository countryRepository;
    private StateRepository stateRepository;
    private LGARepository lgaRepository;
    private UserRepository userRepository;

    private final ProjectOwnerRepository projectOwnerRepository;
    private final ProjectCategoryRepository projectCategoryRepository;
    private final SectorRepository sectorRepository;


    public Validations(RoleRepository roleRepository, CountryRepository countryRepository, StateRepository stateRepository, LGARepository lgaRepository, UserRepository userRepository, ProjectOwnerRepository projectOwnerRepository, ProjectCategoryRepository projectCategoryRepository, SectorRepository sectorRepository) {
        this.roleRepository = roleRepository;
        this.countryRepository = countryRepository;
        this.stateRepository = stateRepository;
        this.lgaRepository = lgaRepository;
        this.userRepository = userRepository;
        this.projectOwnerRepository = projectOwnerRepository;
        this.projectCategoryRepository = projectCategoryRepository;
        this.sectorRepository = sectorRepository;
    }

    public void validateState(StateDto stateDto) {
        if (stateDto.getName() == null || stateDto.getName().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");
        String valName = stateDto.getName();
        char valCharName = valName.charAt(0);
        if (Character.isDigit(valCharName)){
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name can not start with a number");
        }
        Country country = countryRepository.findById(stateDto.getCountryId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid Country id!"));
    }

    public void validateLGA (LGADto lgaDto){
        if (lgaDto.getName() == null || lgaDto.getName().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");

        String valName = lgaDto.getName();
        char valCharName = valName.charAt(0);
        if (Character.isDigit(valCharName)){
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name can not start with a number");
        }

        State state = stateRepository.findById(lgaDto.getStateId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid State id!"));
    }

    public void validateCountry(CountryDto countryDto) {
        if (countryDto.getName() == null || countryDto.getName().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");
        if(countryDto.getCode() == null || countryDto.getCode().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Code cannot be empty");
    }

    public String generateReferenceNumber(int numOfDigits) {
        if (numOfDigits < 1) {
            throw new IllegalArgumentException(numOfDigits + ": Number must be equal or greater than 1");
        }
        long random = (long) Math.floor(Math.random() * 9 * (long) Math.pow(10, numOfDigits - 1)) + (long) Math.pow(10, numOfDigits - 1);
        return Long.toString(random);
    }

    public String generateCode(String code) {
        String encodedString = Base64.getEncoder().encodeToString(code.getBytes());
        return encodedString;
    }

    public void validateEnumerator(EnumeratorSignUpDto enumerator){
        if (enumerator.getIsCorp() == false && (enumerator.getFirstName() == null || enumerator.getFirstName().isEmpty()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "First name cannot be empty");
        if (enumerator.getIsCorp() == false && (enumerator.getFirstName().length() < 2 || enumerator.getFirstName().length() > 100))// NAME LENGTH*********
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid first name  length");
        if (enumerator.getIsCorp() == false && (enumerator.getLastName() == null || enumerator.getLastName().isEmpty()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Last name cannot be empty");
        if (enumerator.getIsCorp() == false && (enumerator.getLastName().length() < 2 || enumerator.getLastName().length() > 100))// NAME LENGTH*********
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid last name  length");

        if (enumerator.getEmail() == null || enumerator.getEmail().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "email cannot be empty");
        if (!Utility.validEmail(enumerator.getEmail().trim()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid Email Address");
        if (enumerator.getPhone() == null || enumerator.getPhone().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Phone number cannot be empty");
        if (enumerator.getPhone().length() < 8 || enumerator.getPhone().length() > 14)// NAME LENGTH*********
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid phone number  length");
        if (!Utility.isNumeric(enumerator.getPhone()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for phone number ");
        if (enumerator.getIsCorp() == true && (enumerator.getCorporateName() == null || enumerator.getCorporateName().isEmpty()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");
    }

    public void validateEnumeratorProperties(CompleteSignupRequest enumeratorPropertiesDto) {
        if (enumeratorPropertiesDto.getIsCorp() == true && (enumeratorPropertiesDto.getCorporateName() == null || enumeratorPropertiesDto.getCorporateName().isEmpty()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");

        if (enumeratorPropertiesDto.getAddress() == null || enumeratorPropertiesDto.getAddress().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Address cannot be empty");
        LGA lga = lgaRepository.findById(enumeratorPropertiesDto.getLgaId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid LGA id!"));
        if (enumeratorPropertiesDto.getPhone() == null || enumeratorPropertiesDto.getPhone().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Phone cannot be empty");
        if (enumeratorPropertiesDto.getEmail() == null || enumeratorPropertiesDto.getEmail().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Email cannot be empty");
    }

    public void validateEnumeratorUpdate(EnumeratorDto enumeratorPropertiesDto) {
        if (enumeratorPropertiesDto.getIsCorp() == true && (enumeratorPropertiesDto.getCorporateName() == null || enumeratorPropertiesDto.getCorporateName().isEmpty()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");

        if (enumeratorPropertiesDto.getAddress() == null || enumeratorPropertiesDto.getAddress().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Address cannot be empty");
        LGA lga = lgaRepository.findById(enumeratorPropertiesDto.getLgaId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid LGA id!"));
        if (enumeratorPropertiesDto.getPhone() == null || enumeratorPropertiesDto.getPhone().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Phone cannot be empty");
        if (enumeratorPropertiesDto.getEmail() == null || enumeratorPropertiesDto.getEmail().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Email cannot be empty");
    }

    public void validateOrganisationType(OrganisationTypeDto organisationTypeDto) {
        if (organisationTypeDto.getName() == null || organisationTypeDto.getName().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");
        if (organisationTypeDto.getDescription() == null || organisationTypeDto.getDescription().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Description cannot be empty");
    }

    public void validateProjectOwnerSignUp(ProjectOwnerSignUpDto projectOwnerSignUp) {
        if (projectOwnerSignUp.getIsCorp() == null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Is Corp cannot be empty");
        if (projectOwnerSignUp.getIsCorp() == false && (projectOwnerSignUp.getFirstName() == null || projectOwnerSignUp.getFirstName().isEmpty()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "First name cannot be empty");
        if (projectOwnerSignUp.getIsCorp() == false && (projectOwnerSignUp.getFirstName().length() < 2 || projectOwnerSignUp.getFirstName().length() > 100))// NAME LENGTH*********
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid first name  length");
        if (projectOwnerSignUp.getIsCorp() == false && (projectOwnerSignUp.getLastName() == null || projectOwnerSignUp.getLastName().isEmpty()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Last name cannot be empty");
        if (projectOwnerSignUp.getIsCorp() == false && (projectOwnerSignUp.getLastName().length() < 2 || projectOwnerSignUp.getLastName().length() > 100))// NAME LENGTH*********
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid last name  length");

        if (projectOwnerSignUp.getEmail() == null || projectOwnerSignUp.getEmail().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "email cannot be empty");
        if (!Utility.validEmail(projectOwnerSignUp.getEmail().trim()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid Email Address");
        if (projectOwnerSignUp.getPhone() == null || projectOwnerSignUp.getPhone().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Phone number cannot be empty");
        if (projectOwnerSignUp.getPhone().length() < 8 || projectOwnerSignUp.getPhone().length() > 14)// NAME LENGTH*********
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid phone number  length");
        if (!Utility.isNumeric(projectOwnerSignUp.getPhone()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for phone number ");
        if (projectOwnerSignUp.getIsCorp() == true && (projectOwnerSignUp.getCorporateName() == null || projectOwnerSignUp.getCorporateName().isEmpty()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Co operate Name cannot be empty");
    }

    public void validateProjectCategory(ProjectCategoryDto projectCategoryDto) {
        if (projectCategoryDto.getName() == null || projectCategoryDto.getName().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");
        if (projectCategoryDto.getDescription() == null || projectCategoryDto.getDescription().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Description cannot be empty");

        projectOwnerRepository.findById(projectCategoryDto.getProjectOwnerId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid Project Owner id!"));
    }

    public void validateSector(SectorDto sectorDto) {
        if (sectorDto.getName() == null || sectorDto.getName().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");
    }

    public void validateProject(ProjectDto projectDto) {
        if (projectDto.getName() == null || projectDto.getName().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");

        if (projectDto.getIsLocationBased() == null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Is location based cannot be empty");

        if (!EnumUtils.isValidEnum(Status.class, projectDto.getStatus()))
            throw new BadRequestException(CustomResponseCode.NOT_FOUND_EXCEPTION, "Enter a valid value for status");

        if (projectDto.getStartDate() == null || projectDto.getStartDate().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Start date cannot be empty");

        if (projectDto.getEndDate() == null || projectDto.getEndDate().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "End date cannot be empty");

        projectOwnerRepository.findById(projectDto.getProjectOwnerId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid Project Owner id!"));
        projectCategoryRepository.findById(projectDto.getProjectCategoryId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid Project Category id!"));
        sectorRepository.findById(projectDto.getSectorId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid Sector id!"));
    }

    public void validateDataSet(DataSetDto dataSetDto) {
        if (dataSetDto.getName() == null && (dataSetDto.getName().isEmpty()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");
    }

    public void validateCommentDictionary(CommentDictionaryDto commentDictionaryDto) {
        System.err.println(commentDictionaryDto);
        if (commentDictionaryDto.getName() == null || commentDictionaryDto.getName().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");
        if (commentDictionaryDto.getAdditionalInfo() == null || commentDictionaryDto.getAdditionalInfo().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Additional Info cannot be empty");
    }

    public void validateIndicatorDictionary(IndicatorDictionaryDto indicatorDictionaryDto) {
        if (indicatorDictionaryDto.getName() == null || indicatorDictionaryDto.getName().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");
        if (indicatorDictionaryDto.getAdditionalInfo() == null || indicatorDictionaryDto.getAdditionalInfo().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Additional Info cannot be empty");
    }

    public void validateProjectOwnerUser(ProjectOwnerUserDto projectOwnerUserDto) {
        if (projectOwnerUserDto.getUserId() == null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "User Id cannot be empty");
        if (projectOwnerUserDto.getProjectOwnerId() == null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Project Owner Id cannot be empty");
    }

}


