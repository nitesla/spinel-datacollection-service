package com.spinel.datacollection.service.helper;


import com.spinel.datacollection.core.dto.request.*;
import com.spinel.datacollection.core.enums.*;
import com.spinel.datacollection.core.models.Country;
import com.spinel.datacollection.core.models.LGA;
import com.spinel.datacollection.core.models.ProjectOwner;
import com.spinel.datacollection.core.models.State;
import com.spinel.datacollection.service.repositories.*;
import com.spinel.framework.dto.requestDto.ChangePasswordDto;
import com.spinel.framework.dto.requestDto.CreateTransactionPinDto;
import com.spinel.framework.dto.requestDto.GeneratePassword;
import com.spinel.framework.exceptions.BadRequestException;
import com.spinel.framework.exceptions.ConflictException;
import com.spinel.framework.exceptions.NotFoundException;
import com.spinel.framework.models.Role;
import com.spinel.framework.models.User;
import com.spinel.framework.repositories.RoleRepository;
import com.spinel.framework.repositories.UserRepository;
import com.spinel.framework.utils.CustomResponseCode;
import com.spinel.framework.utils.Utility;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

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
    private final IndicatorDictionaryRepository indicatorDictionaryRepository;
    private final DataSetRepository dataSetRepository;
    private final DataUserRepository dataUserRepository;
    private final ProjectRoleRepository projectRoleRepository;
    private final DataWalletRepository walletRepository;
    private final UserBankRepository userBankRepository;

    //private final FormRepository formRepository;

    @Autowired
    private OrganisationTypeRepository organisationTypeRepository;

    @Autowired
    private EnumeratorProjectRepository enumeratorProjectRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private EnumeratorRepository enumeratorRepository;

    @Autowired
    private ProjectLocationRepository projectLocationRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private CommentDictionaryRepository commentDictionaryRepository;

    @Autowired
    private BankRepository bankRepository;

//    @Autowired
//    private FormRepository formRepository;

//    @Autowired
//    private WalletRepository walletRepository;

    public void validateDataUser(DataCollectionUserRequestDto request){
        if (request.getFirstName() == null || request.getFirstName().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "First name cannot be empty");
        if (request.getFirstName().length() < 2 || request.getFirstName().length() > 100)// NAME LENGTH*********
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid first name  length");

        if (request.getLastName() == null || request.getLastName().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Last name cannot be empty");
        if (request.getLastName().length() < 2 || request.getLastName().length() > 100)// NAME LENGTH*********
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid last name  length");

        if (request.getEmail() == null || request.getEmail().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "email cannot be empty");
        if (!Utility.validEmail(request.getEmail().trim()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid Email Address");
        User user = userRepository.findByEmail(request.getEmail());
        if(user !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Email already exist");
        }
        if (request.getPhone() == null || request.getPhone().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Phone number cannot be empty");
        if (request.getPhone().length() < 8 || request.getPhone().length() > 14)// NAME LENGTH*********
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid phone number  length");
        if (!Utility.isNumeric(request.getPhone()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for phone number ");
        User userExist = userRepository.findByPhone(request.getPhone());
        if(userExist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "  user phone already exist");
        }
        if(request.getRoleId() == null){
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Role id cannot be empty");
        }

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid role id!"));

    }


    public Validations(RoleRepository roleRepository, CountryRepository countryRepository, StateRepository stateRepository, LGARepository lgaRepository, UserRepository userRepository, ProjectOwnerRepository projectOwnerRepository, ProjectCategoryRepository projectCategoryRepository, SectorRepository sectorRepository, IndicatorDictionaryRepository indicatorDictionaryRepository, DataSetRepository dataSetRepository, DataUserRepository dataUserRepository, ProjectRoleRepository projectRoleRepository, DataWalletRepository walletRepository, UserBankRepository userBankRepository) {
        this.roleRepository = roleRepository;
        this.countryRepository = countryRepository;
        this.stateRepository = stateRepository;
        this.lgaRepository = lgaRepository;
        this.userRepository = userRepository;
        this.projectOwnerRepository = projectOwnerRepository;
        this.projectCategoryRepository = projectCategoryRepository;
        this.sectorRepository = sectorRepository;
        this.indicatorDictionaryRepository = indicatorDictionaryRepository;
        this.dataSetRepository = dataSetRepository;
        this.dataUserRepository = dataUserRepository;
        this.projectRoleRepository = projectRoleRepository;
        this.walletRepository = walletRepository;
        this.userBankRepository = userBankRepository;
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
//        if (enumerator.getUserBankId() == null)
//            throw new BadRequestException(CustomResponseCode.BAD_REQUEST,"The UserBankId cannot be missing");
//        userBankRepository.findById(enumerator.getUserBankId())
//                .orElseThrow(()-> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"The given UserBankId doesn't exist"));

        organisationTypeRepository.findById(enumerator.getOrganisationTypeId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, "Enter a valid Organisation type id!"));

        projectRoleRepository.findById(enumerator.getProjectRoleId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Enter a valid Role id!"));
    }

    public void validateEnumeratorProperties(CompleteSignupRequest enumeratorPropertiesDto) {
        if (enumeratorPropertiesDto.getIsCorp() == null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Is Corp cannot be empty");

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
        if (!EnumUtils.isValidEnum(Gender.class, enumeratorPropertiesDto.getGender().toUpperCase()))
            throw new BadRequestException(CustomResponseCode.NOT_FOUND_EXCEPTION, "Enter a valid value for gender: MALE/FEMALE/OTHERS");
        if (!EnumUtils.isValidEnum(EnumeratorVerificationStatus.class, enumeratorPropertiesDto.getVerificationStatus().toUpperCase()))
            throw new BadRequestException(CustomResponseCode.NOT_FOUND_EXCEPTION, "Enter a valid value for verificationStatus: PENDING/VERIFIED/UNVERIFIED");
        countryRepository.findById(enumeratorPropertiesDto.getCountryId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid Country id!"));
        organisationTypeRepository.findById(enumeratorPropertiesDto.getOrganisationTypeId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, "Enter a valid Organisation type id!"));
    }

    public void validateEnumeratorUpdate(EnumeratorDto enumeratorPropertiesDto) {
        if (enumeratorPropertiesDto.getIsCorp() == true && (enumeratorPropertiesDto.getCorporateName() == null || enumeratorPropertiesDto.getCorporateName().isEmpty()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");
        if (enumeratorPropertiesDto.getIsCorp() == false && (enumeratorPropertiesDto.getFirstName() == null || enumeratorPropertiesDto.getFirstName().isEmpty()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "First name cannot be empty");
        if (enumeratorPropertiesDto.getIsCorp() == false && (enumeratorPropertiesDto.getFirstName().length() < 2 || enumeratorPropertiesDto.getFirstName().length() > 100))// NAME LENGTH*********
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid first name  length");
        if (enumeratorPropertiesDto.getIsCorp() == false && (enumeratorPropertiesDto.getLastName() == null || enumeratorPropertiesDto.getLastName().isEmpty()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Last name cannot be empty");
        if (enumeratorPropertiesDto.getIsCorp() == false && (enumeratorPropertiesDto.getLastName().length() < 2 || enumeratorPropertiesDto.getLastName().length() > 100))// NAME LENGTH*********
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid last name  length");

        if (enumeratorPropertiesDto.getAddress() == null || enumeratorPropertiesDto.getAddress().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Address cannot be empty");
        if (enumeratorPropertiesDto.getUserBankId() == null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "The userBankId cannot be null");
        userBankRepository.findById(enumeratorPropertiesDto.getUserBankId())
                .orElseThrow(()-> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"The given UserBankId doesn't exist"));

        LGA lga = lgaRepository.findById(enumeratorPropertiesDto.getLgaId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid LGA id!"));
        organisationTypeRepository.findById(enumeratorPropertiesDto.getOrganisationTypeId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid Organisation Type id!"));
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
//        if (projectOwnerSignUp.getUserBankId() == null)
//            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "UserBankId cannot be null");
//        userBankRepository.findById(projectOwnerSignUp.getUserBankId())
//                .orElseThrow(()-> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"The given UserBankId doesn't exist"));

        User userPhone = userRepository.findByPhone(projectOwnerSignUp.getPhone());
        if (userPhone != null )
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "User with Phone number already exists");

        User userEmail = userRepository.findByEmail(projectOwnerSignUp.getEmail());
        if (userEmail != null )
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "User with email already exists");

        sectorRepository.findById(projectOwnerSignUp.getSectorId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Sector Id does not exist!"));
    }

    public void validateProjectOwnerCompleteSignUp(CompleteSignupProjectOwnerRequest request) {
        ProjectOwner projectOwnerEmail = projectOwnerRepository.findProjectOwnerByEmail(request.getEmail());
        if (projectOwnerEmail != null )
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "Project owner with email already exists");

        ProjectOwner projectOwnerPhone = projectOwnerRepository.findProjectOwnerByPhone(request.getPhone());
        if (projectOwnerPhone != null )
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "Project owner with Phone number already exists");

        countryRepository.findById(request.getCountryId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Country Id does not exist!"));
        stateRepository.findById(request.getStateId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested State Id does not exist!"));
        lgaRepository.findById(request.getLgaId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested LGA Id does not exist!"));

        sectorRepository.findById(request.getSectorId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Sector Id does not exist!"));
    }

    public void validateCreateProjectOwner(ProjectOwnerDto projectOwnerDto) {
        ProjectOwner projectOwnerEmail = projectOwnerRepository.findProjectOwnerByEmail(projectOwnerDto.getEmail());
        if (projectOwnerEmail != null )
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "Project owner with email already exists");

        ProjectOwner projectOwnerPhone = projectOwnerRepository.findProjectOwnerByPhone(projectOwnerDto.getPhone());
        if (projectOwnerPhone != null )
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "Project owner with Phone number already exists");
        sectorRepository.findById(projectOwnerDto.getSectorId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Sector Id does not exist!"));
        lgaRepository.findById(projectOwnerDto.getLgaId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested lga Id does not exist!"));


        if (projectOwnerDto.getIsCorp() == null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Is Corp cannot be empty");
        if (projectOwnerDto.getIsCorp() == false && (projectOwnerDto.getFirstname() == null || projectOwnerDto.getFirstname().isEmpty()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "First name cannot be empty");
        if (projectOwnerDto.getIsCorp() == false && (projectOwnerDto.getFirstname().length() < 2 || projectOwnerDto.getFirstname().length() > 100))// NAME LENGTH*********
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid first name  length");
        if (projectOwnerDto.getIsCorp() == false && (projectOwnerDto.getLastname() == null || projectOwnerDto.getLastname().isEmpty()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Last name cannot be empty");
        if (projectOwnerDto.getIsCorp() == false && (projectOwnerDto.getLastname().length() < 2 || projectOwnerDto.getLastname().length() > 100))// NAME LENGTH*********
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid last name  length");

        if (projectOwnerDto.getEmail() == null || projectOwnerDto.getEmail().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "email cannot be empty");
        if (!Utility.validEmail(projectOwnerDto.getEmail().trim()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid Email Address");
        if (projectOwnerDto.getPhone() == null || projectOwnerDto.getPhone().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Phone number cannot be empty");
        if (projectOwnerDto.getPhone().length() < 8 || projectOwnerDto.getPhone().length() > 14)// NAME LENGTH*********
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid phone number  length");
        if (!Utility.isNumeric(projectOwnerDto.getPhone()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for phone number ");
        if (projectOwnerDto.getIsCorp() == true && (projectOwnerDto.getCorporateName() == null || projectOwnerDto.getCorporateName().isEmpty()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Co operate Name cannot be empty");

    }

    public void validateUpdateProjectOwner(UpdateProjectOwnerDto request) {
        sectorRepository.findById(request.getSectorId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Sector Id does not exist!"));

        if (request.getEmail() == null || request.getEmail().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "email cannot be empty");
        if (request.getFirstname() == null || request.getFirstname().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "firstname cannot be empty");
        if (!Utility.validateName(request.getFirstname()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for First Name ");
        if (request.getLastname() == null || request.getLastname().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "lastname cannot be empty");
        if (!Utility.validateName(request.getLastname()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid data type for Last Name ");
        if (request.getPhone() == null || request.getPhone().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "phone cannot be empty");
        if (Objects.nonNull(request.getGender()) && !EnumUtils.isValidEnum(Gender.class, request.getGender().toUpperCase()))
            throw new BadRequestException(CustomResponseCode.NOT_FOUND_EXCEPTION, "Enter a valid value for gender: MALE/FEMALE/OTHERS");
        countryRepository.findById(request.getCountryId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Country Id does not exist!"));
        stateRepository.findById(request.getStateId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested State Id does not exist!"));
        lgaRepository.findById(request.getLgaId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested LGA Id does not exist!"));


        ProjectOwner projectOwnerPhone = projectOwnerRepository.findProjectOwnerByPhone(request.getPhone());
        if(Objects.nonNull(projectOwnerPhone) && projectOwnerPhone.getId() != request.getId())
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "Project Owner with Phone number already exists");

        ProjectOwner projectOwnerEmail = projectOwnerRepository.findProjectOwnerByEmail(request.getEmail());
        if(Objects.nonNull(projectOwnerEmail) && projectOwnerEmail.getId() != request.getId())
                throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "Project Owner with email already exists");

    }

    public void validateProjectCategory(ProjectCategoryDto projectCategoryDto) {
        if (projectCategoryDto.getName() == null || projectCategoryDto.getName().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");
        if (projectCategoryDto.getDescription() == null || projectCategoryDto.getDescription().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Description cannot be empty");
    }

    public void validateSector(SectorDto sectorDto) {
        if (sectorDto.getName() == null || sectorDto.getName().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");
    }

    public void validateProject(ProjectDto projectDto) {
        if (projectDto.getName() == null || projectDto.getName().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");

        List<Long> projectCategories = projectDto.getProjectCategoryIds();
        if (projectCategories.size() > 3) {
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Project categories cannot be more than 3");
        }

        for(Long projectCategoryId: projectDto.getProjectCategoryIds()) {
            projectCategoryRepository.findById(projectCategoryId)
                    .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, "Invalid Project CategoryId: " + projectCategoryId));
        }
        projectOwnerRepository.findById(projectDto.getProjectOwnerId())
        .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                " Enter a valid Project Owner id!"));
        if (Objects.isNull(projectDto.getStatus()) || !EnumUtils.isValidEnum(Status.class, projectDto.getStatus().toUpperCase()))
            throw new BadRequestException(CustomResponseCode.NOT_FOUND_EXCEPTION, "Enter a valid value for status");

//        if (projectDto.getIsLocationBased() == null)
//            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Is location based cannot be empty");
//
//
//
//        if (projectDto.getStartDate() == null || projectDto.getStartDate().isEmpty())
//            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Start date cannot be empty");
//
//        if (projectDto.getEndDate() == null || projectDto.getEndDate().isEmpty())
//            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "End date cannot be empty");
//
//        projectCategoryRepository.findById(projectDto.getProjectCategoryId())
//                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
//                        " Enter a valid Project Category id!"));
//        sectorRepository.findById(projectDto.getSectorId())
//                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
//                        " Enter a valid Sector id!"));
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

    public void validateEnumeratorRating(EnumeratorRatingDto request) {
        if (request.getEnumeratorProjectId() == null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "enumeratorProjectId cannot be empty");
        if (request.getRating() == null )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Rating cannot be empty");

        enumeratorProjectRepository.findById(request.getEnumeratorProjectId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid Enumerator Project Id!"));
    }

    public void validateEnumeratorProject(EnumeratorProjectDto request) {
        if (request.getProjectId() == null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "projectId cannot be empty");
        if (request.getEnumeratorId() == null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "enumeratorId cannot be empty");
        if (request.getAssignedDate() == null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "assignedDate cannot be empty");
        if (request.getCompletedDate() == null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "completedDate cannot be empty");
        if (request.getStatus() == null && request.getStatus().toString().isEmpty() )
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Status cannot be empty");

        projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid Project Id!"));

        enumeratorRepository.findById(request.getEnumeratorId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid Enumerator Id!"));
    }

    public void validateEnumeratorProjectLocation(EnumeratorProjectLocationDto request) {
        if (request.getEnumeratorProjectId() == null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "enumeratorProjectId cannot be empty");
        if (request.getProjectLocationId() == null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "projectLocationId cannot be empty");
        if (request.getCollectedRecord() == null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "collectedRecord cannot be empty");
        if (request.getExpectedRecord() == null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "expectedRecord cannot be empty");

        projectLocationRepository.findById(request.getProjectLocationId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid Project Location Id!"));

        enumeratorProjectRepository.findById(request.getEnumeratorProjectId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid Enumerator Project Id!"));
    }

    public void validatePricingConfiguration(PricingConfigurationDto pricingConfigurationDto){
        if (pricingConfigurationDto.getPrice() == null ){
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Price cannot be empty");
        }
        dataSetRepository.findById(pricingConfigurationDto.getDataSetId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid Dataset id!"));
    }

    public void validateProjectLocation(ProjectLocationDto projectLocationDto) {
        if (projectLocationDto.getLocationType() == null || projectLocationDto.getLocationType().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Location Type cannot be empty");
        if (projectLocationDto.getLocationType().equalsIgnoreCase(Location.STATE.toString())) {
            stateRepository.findById(projectLocationDto.getLocationId())
                    .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                            "Enter valid location Id"));
        }
        if (projectLocationDto.getLocationType().equalsIgnoreCase(Location.LGA.toString())) {
            lgaRepository.findById(projectLocationDto.getLocationId())
                    .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                            "Enter valid location Id"));
        }
        if (projectLocationDto.getName() == null || projectLocationDto.getName().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");
        projectRepository.findById(projectLocationDto.getProjectId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Enter valid project Id"));
    }

    public void validateProjectIndicator(ProjectIndicatorDto projectIndicatorDto) {
        projectRepository.findById(projectIndicatorDto.getProjectId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Enter valid project Id"));
        indicatorDictionaryRepository.findById(projectIndicatorDto.getIndicatorId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Enter valid Indicaator Id"));
    }

    public void validateProjectOwnerUser(ProjectOwnerUserDto projectOwnerUserDto) {
        userRepository.findById(projectOwnerUserDto.getUserId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Enter valid user Id"));
        projectOwnerRepository.findById(projectOwnerUserDto.getProjectOwnerId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Enter valid project owner Id"));
    }

    public void validateSubmissionComment(SubmissionCommentDto request) {
        if (request.getSubmissionId() == null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "submissionId cannot be empty");
        if (request.getCommentId() == null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "commentId cannot be empty");

        submissionRepository.findById(request.getSubmissionId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid Submission Id!"));

        commentDictionaryRepository.findById(request.getCommentId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid Comment Id!"));
    }

    public void validateProjectEnumerator(ProjectEnumeratorRequestDto projectEnumeratorRequestDto) {
        if (projectEnumeratorRequestDto.getEnumeratorId() == null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST,"enumeratorId can not be empty");
        if (projectEnumeratorRequestDto.getProjectId() == null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST,"ProjectId cannot be empty");
        projectRepository.findById(projectEnumeratorRequestDto.getProjectId())
                .orElseThrow(()->new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"Enter a valid Project Id"));
        enumeratorRepository.findById(projectEnumeratorRequestDto.getEnumeratorId())
                .orElseThrow(()->new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, "Enter a valid Enumerator Id"));
    }

    public void validateProjectForm(ProjectFormRequestDto projectFormRequestDto) {
        if (projectFormRequestDto.getFormId() == null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST,"FormId cannot be empty ");
        if (projectFormRequestDto.getProjectId() == null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST,"ProjectId cannot be empty");
        projectRepository.findById(projectFormRequestDto.getProjectId())
                .orElseThrow(()->new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"Enter a valid Project Id"));

        //formRepository.findById(projectFormRequestDto.getFormId())
        //        .orElseThrow(()->new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"Enter a valid Form Id"));

    }
    public void validateProjectBilling(ProjectBillingRequestDto projectBillingRequestDto) {
        if (projectBillingRequestDto.getProjectId() == null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST,"ProjectId cannot be empty");
        // Validate other parameters as needed and as introduced in the ProjectBilling Model.
        projectRepository.findById(projectBillingRequestDto.getProjectId())
                .orElseThrow(()->new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"Enter a valid project Id"));
        // Validate other parameters as needed and as introduced in the ProjectBilling Model.
    }

    public void validateSubmission(SubmissionDto request) {
        if (request.getProjectId() == null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "projectId cannot be empty");
        if (request.getCommentId() == null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "commentId cannot be empty");

        if (request.getFormId() == null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "formId cannot be empty");

        if (request.getEnumeratorId() == null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "enumeratorId cannot be empty");

        enumeratorRepository.findById(request.getEnumeratorId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid Enumerator Id!"));

        commentDictionaryRepository.findById(request.getCommentId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid Comment Id!"));
        validateSubmissionStatus(request.getStatus().toString());

//        formRepository.findById(request.getFormId())
//                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
//                        " Enter a valid Form Id!"));
    }

    public void validateForm(FormDto request) {
        if (request.getName() == null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "name cannot be empty");
//        if (request.getVersion() == null)
//            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "version cannot be empty");
        if (request.getDescription() == null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "description cannot be empty");
        userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid User id!"));
    }

    public void validateTransaction(TransactionDto request) {
        if (request.getHash() == null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "hash field cannot be empty");

        if (request.getWalletId() == null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "walletId cannot be empty");
        if (request.getAmount() == null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "amount cannot be empty");
        if (request.getInitialBalance() == null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "initBalance cannot be empty");
        if (request.getFinalBalance() == null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "finalBalance cannot be empty");
        if (request.getSenderId() == null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "senderId cannot be empty");
        if (request.getReceiverId() == null)
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "receiverId cannot be empty");
        walletRepository.findById(request.getWalletId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid Wallet Id!"));
        userRepository.findById(request.getSenderId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid valid Sender Id!"));
        userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid Receiver Id!"));
    }

    public void validateBank(BankDto bankDto) {
        String valName = bankDto.getName();
        char valCharName = valName.charAt(0);
        if (Character.isDigit(valCharName)){
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name can not start with a number");
        }
        if (bankDto.getName() == null || bankDto.getName().trim().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");
        if (bankDto.getCode() == null || bankDto.getCode().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Bank code cannot be empty");
    }

    public void validateTransactionPin(CreateTransactionPinDto transactionPinDto) {
        if (transactionPinDto.getTransactionPin() == null || transactionPinDto.getTransactionPin().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Transaction pin cannot be empty");

        if (!Utility.isNumeric(transactionPinDto.getTransactionPin()))
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Transaction pin must be numeric ");

        if (transactionPinDto.getTransactionPin().length() < 4 || transactionPinDto.getTransactionPin().length() > 6)// LENGTH*********
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid pin length");
    }


    public void changePassword(ChangePasswordDto changePasswordDto) {
        if (changePasswordDto.getPassword() == null || changePasswordDto.getPassword().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Password cannot be empty");
        if (changePasswordDto.getPassword().length() < 6 || changePasswordDto.getPassword().length() > 20)// NAME LENGTH*********
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid password length");
        if (changePasswordDto.getPreviousPassword() == null || changePasswordDto.getPreviousPassword().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Previous password cannot be empty");


    }

    public void generatePasswordValidation(GeneratePassword request) {

        if (request.getPhone() == null || request.getPhone().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Phone cannot be empty");

    }

    public void validateProjectStatus(String status) {
        if(!Arrays.stream(Status.values()).anyMatch((t) -> t.name().equals(status))){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, "Invalid value for status!");
        }
    }

    public void validateSubmissionStatus(String status) {
        if(!Arrays.stream(SubmissionStatus.values()).anyMatch((t) -> t.name().equals(status))){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, "Invalid value for status!");
        }
    }

    public void validateProjectRole(ProjectRoleDto request) {
        if (request.getName() == null || request.getName().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");
    }

    public void validateuserBank(UserBankRequestDto request) {
        bankRepository
                .findById(request.getBankId()).orElseThrow(() ->
                new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, "Bank not found"));
        userRepository
                .findById(request.getUserId()).orElseThrow(()->
                new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, "User not found"));
    }

    public void validateJobRequest(JobRequestDto request) {
        userRepository
                .findById(request.getUserId()).orElseThrow(()->
                new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, "User not found"));
        projectRepository
                .findById(request.getProjectId()).orElseThrow(() ->
                new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, "Project not found"));
    }
}


