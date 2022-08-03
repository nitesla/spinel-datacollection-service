package com.sabi.datacollection.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sabi.datacollection.core.dto.request.CompleteSignupRequest;
import com.sabi.datacollection.core.dto.request.EnableDisableDto;
import com.sabi.datacollection.core.dto.request.EnumeratorDto;
import com.sabi.datacollection.core.dto.request.EnumeratorSignUpDto;
import com.sabi.datacollection.core.dto.response.CompleteSignUpResponse;
import com.sabi.datacollection.core.dto.response.EnumeratorActivationResponse;
import com.sabi.datacollection.core.dto.response.EnumeratorResponseDto;
import com.sabi.datacollection.core.dto.response.EnumeratorSignUpResponseDto;
import com.sabi.datacollection.core.enums.EnumeratorVerificationStatus;
import com.sabi.datacollection.core.enums.EnumeratorStatus;
import com.sabi.datacollection.core.enums.UserCategory;
import com.sabi.datacollection.core.models.*;
import com.sabi.datacollection.service.helper.DateFormatter;
import com.sabi.datacollection.service.helper.Validations;
import com.sabi.datacollection.service.repositories.*;
import com.sabi.framework.dto.requestDto.ChangePasswordDto;
import com.sabi.framework.exceptions.BadRequestException;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.PreviousPasswords;
import com.sabi.framework.models.User;
import com.sabi.framework.models.UserRole;
import com.sabi.framework.notification.requestDto.NotificationRequestDto;
import com.sabi.framework.notification.requestDto.RecipientRequest;
import com.sabi.framework.notification.requestDto.SmsRequest;
import com.sabi.framework.notification.requestDto.WhatsAppRequest;
import com.sabi.framework.repositories.PreviousPasswordRepository;
import com.sabi.framework.repositories.UserRepository;
import com.sabi.framework.repositories.UserRoleRepository;
import com.sabi.framework.service.AuditTrailService;
import com.sabi.framework.service.NotificationService;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.service.WhatsAppService;
import com.sabi.framework.utils.AuditTrailFlag;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.framework.utils.Utility;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;


@SuppressWarnings("ALL")
@Slf4j
@Service
public class EnumeratorService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OrganisationTypeRepository organisationTypeRepository;

    @Autowired
    private WhatsAppService whatsAppService;

    private EnumeratorRepository repository;
    private UserRepository userRepository;
    private PreviousPasswordRepository previousPasswordRepository;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;
    private final Validations validations;
    private NotificationService notificationService;
    private LGARepository lgaRepository;
    private final AuditTrailService auditTrailService;
    private final StateRepository stateRepository;
    private final UserRoleRepository userRoleRepository;
    private final ProjectEnumeratorService projectEnumeratorService;
    private final CountryRepository countryRepository;
    private final ProjectRoleRepository projectRoleRepository;
    private final SubmissionService submissionService;


    public EnumeratorService(EnumeratorRepository repository, UserRepository userRepository,
                             PreviousPasswordRepository previousPasswordRepository, ModelMapper mapper,
                             ObjectMapper objectMapper, Validations validations, NotificationService notificationService,
                             LGARepository lgaRepository, AuditTrailService auditTrailService,
                             StateRepository stateRepository, UserRoleRepository userRoleRepository, ProjectEnumeratorService projectEnumeratorService,
                             CountryRepository countryRepository, ProjectRoleRepository projectRoleRepository, SubmissionService submissionService) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.previousPasswordRepository = previousPasswordRepository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.validations = validations;
        this.notificationService = notificationService;
        this.lgaRepository = lgaRepository;
        this.auditTrailService = auditTrailService;
        this.stateRepository = stateRepository;
        this.userRoleRepository = userRoleRepository;
        this.projectEnumeratorService = projectEnumeratorService;
        this.countryRepository = countryRepository;
        this.projectRoleRepository = projectRoleRepository;
        this.submissionService = submissionService;
    }




    public EnumeratorSignUpResponseDto enumeratorSignUp(EnumeratorSignUpDto request, HttpServletRequest request1) {
        validations.validateEnumerator(request);
        User user = mapper.map(request,User.class);

        User exist = userRepository.findByEmailOrPhone(request.getEmail(),request.getPhone());
        if(exist !=null && exist.getPasswordChangedOn()== null){

            Enumerator enumeratorExist = repository.findByUserId(exist.getId());
            if(enumeratorExist !=null){
                EnumeratorSignUpResponseDto enumeratorSignUpResponseDto= EnumeratorSignUpResponseDto.builder()
                  .id(exist.getId())
                  .email(exist.getEmail())
                  .firstName(exist.getFirstName())
                  .lastName(exist.getLastName())
                  .phone(exist.getPhone())
                  .username(exist.getUsername())
                  .enumeratorId(enumeratorExist.getId())
                  .build();
          return enumeratorSignUpResponseDto;
            }else {
                throw new BadRequestException(CustomResponseCode.BAD_REQUEST, " Enumerator id does not exist");
            }

        }else if(exist !=null && exist.getPasswordChangedOn() !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Enumerator user already exist");
        }
        String password = request.getPassword();
        user.setPassword(passwordEncoder.encode(password));
        user.setUserCategory(UserCategory.ENUMERATOR.toString());
        user.setUsername(request.getEmail());
        user.setLoginAttempts(0);
        user.setResetToken(Utility.registrationCode("HHmmss"));
        user.setResetTokenExpirationDate(Utility.tokenExpiration());
        user.setCreatedBy(0l);
        user.setIsActive(false);
        user = userRepository.save(user);
        log.debug("Create new agent user - {}"+ new Gson().toJson(user));

        UserRole userRole = UserRole.builder()
                .userId(user.getId())
                .roleId(user.getRoleId())
                .build();
        userRoleRepository.save(userRole);

        PreviousPasswords previousPasswords = PreviousPasswords.builder()
                .userId(user.getId())
                .password(user.getPassword())
                .createdDate(LocalDateTime.now())
                .build();
        previousPasswordRepository.save(previousPasswords);

        Enumerator saveEnumerator = new Enumerator();
        saveEnumerator.setFirstName(request.getFirstName());
        saveEnumerator.setLastName(request.getLastName());
        saveEnumerator.setProjectRoleId(request.getProjectRoleId());
        saveEnumerator.setUserId(user.getId());
        saveEnumerator.setIsActive(false);
        saveEnumerator.setCreatedBy(user.getId());
        saveEnumerator.setCorp(request.getIsCorp());
        saveEnumerator.setStatus(EnumeratorStatus.PENDING);
        if (request.getIsCorp() == true){
            saveEnumerator.setCorporateName(request.getCorporateName());
        }

        Enumerator enumeratorResponse= repository.save(saveEnumerator);
        log.debug("Create new Enumerator  - {}"+ new Gson().toJson(saveEnumerator));


        EnumeratorSignUpResponseDto response = EnumeratorSignUpResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .phone(user.getPhone())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .corporateName(enumeratorResponse.getCorporateName())
                .enumeratorId(enumeratorResponse.getId())
                .build();

        // --------  sending token  -----------

        NotificationRequestDto notificationRequestDto = new NotificationRequestDto();
        User emailRecipient = userRepository.getOne(user.getId());
        notificationRequestDto.setMessage("Activation Otp " + " " + user.getResetToken());
        List<RecipientRequest> recipient = new ArrayList<>();
        recipient.add(RecipientRequest.builder()
                .email(emailRecipient.getEmail())
                .build());
        notificationRequestDto.setRecipient(recipient);
        notificationService.emailNotificationRequest(notificationRequestDto);

        SmsRequest smsRequest = SmsRequest.builder()
                .message("Activation Otp " + " " + user.getResetToken())
                .phoneNumber(emailRecipient.getPhone())
                .build();
        notificationService.smsNotificationRequest(smsRequest);


        WhatsAppRequest whatsAppRequest = WhatsAppRequest.builder()
                .message("Activation Otp " + " " + user.getResetToken())
                .phoneNumber(emailRecipient.getPhone())
                .build();
        whatsAppService.whatsAppNotification(whatsAppRequest);

        auditTrailService
                .logEvent(response.getUsername(),
                        "SignUp Enumerator :" + response.getUsername(),
                        AuditTrailFlag.SIGNUP,
                        " Sign up Enumerator Request for:" + user.getFirstName() + " " + user.getLastName() + " " + user.getEmail()
                        , 1, Utility.getClientIp(request1));
        return response;
    }


    public CompleteSignUpResponse completeSignUp(CompleteSignupRequest request, HttpServletRequest request1) {
        validations.validateEnumeratorProperties(request);
        Enumerator enumerator = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Enumerator Id does not exist!"));
        mapper.map(request, enumerator);

        enumerator.setUpdatedBy(enumerator.getUserId());
        enumerator.setIsActive(true);
        enumerator.setStatus(EnumeratorStatus.ACTIVE);
        repository.save(enumerator);
        log.debug("complete signup  - {}"+ new Gson().toJson(enumerator));

        User user = userRepository.getOne(enumerator.getUserId());
        user.setUpdatedBy(enumerator.getUserId());
        user.setIsActive(true);
        userRepository.save(user);

        CompleteSignUpResponse response = CompleteSignUpResponse.builder()
                .enumeratorId(enumerator.getId())
                .email(enumerator.getEmail())
                .corporateName(enumerator.getCorporateName())
                .phone(enumerator.getPhone())
                .address(enumerator.getAddress())
                .registrationDate(enumerator.getRegistrationDate())
                .userId(enumerator.getUserId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .userEmail(user.getEmail())
                .userName(user.getUsername())
                .userPhone(user.getPhone())
                .build();

        auditTrailService
                .logEvent(response.getUserEmail(),
                        "SignUp Enumerator :" + response.getUserEmail(),
                        AuditTrailFlag.SIGNUP,
                        " Sign up Enumerator Request for:" + user.getFirstName() + " " + user.getLastName() + " " + user.getEmail()
                        , 1, Utility.getClientIp(request1));

        return response;

    }

    public EnumeratorActivationResponse enumeratorPasswordActivation(ChangePasswordDto request) {

        User user = userRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested user id does not exist!"));
        mapper.map(request, user);

        String password = request.getPassword();
        user.setPassword(passwordEncoder.encode(password));
        user.setPasswordChangedOn(LocalDateTime.now());
        user.setIsActive(true);
        user = userRepository.save(user);

        PreviousPasswords previousPasswords = PreviousPasswords.builder()
                .userId(user.getId())
                .password(user.getPassword())
                .createdDate(LocalDateTime.now())
                .build();
        previousPasswordRepository.save(previousPasswords);

        Enumerator enumerator = repository.findByUserId(user.getId());

        EnumeratorActivationResponse response = EnumeratorActivationResponse.builder()
                .userId(user.getId())
                .enumeratorId(enumerator.getId())
                .phone(user.getPhone())
                .email(user.getEmail())
                .build();

        return response;
    }

    public EnumeratorResponseDto createEnumeratorProperties(EnumeratorDto request) {
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Enumerator enumeratorProperties = mapper.map(request,Enumerator.class);
        Enumerator exist = repository.findEnumeratorById(request.getId());
        if(exist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Enumerator properties already exist");
        }
        enumeratorProperties.setCreatedBy(userCurrent.getId());
        enumeratorProperties.setIsActive(true);
        enumeratorProperties = repository.save(enumeratorProperties);
        log.debug("Create new enumerator properties - {}"+ new Gson().toJson(enumeratorProperties));
        return mapper.map(enumeratorProperties, EnumeratorResponseDto.class);
    }


    public EnumeratorResponseDto updateEnumeratorProperties(EnumeratorDto request,HttpServletRequest request1) {
        validations.validateEnumeratorUpdate(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Enumerator enumeratorProperties = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested enumerator properties Id does not exist!"));
        mapper.map(request, enumeratorProperties);
        enumeratorProperties.setUpdatedBy(userCurrent.getId());
        repository.save(enumeratorProperties);
        userCurrent.setFirstName(request.getFirstName());
        userCurrent.setLastName(request.getLastName());
        userRepository.save(userCurrent);
        log.debug("enumerator asset record updated - {}"+ new Gson().toJson(enumeratorProperties));
        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Update enumerator by username:" + userCurrent.getUsername(),
                        AuditTrailFlag.UPDATE,
                        " Update enumerator Request for:" + enumeratorProperties.getId() ,1, Utility.getClientIp(request1));
        return mapper.map(enumeratorProperties, EnumeratorResponseDto.class);
    }


    public EnumeratorResponseDto findEnumeratorAsset(Long id){
        Enumerator enumeratorProperties  = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested enumerator properties Id does not exist!"));
        Enumerator enumerator = setTransientFields(enumeratorProperties);
        EnumeratorResponseDto enumeratorResponseDto = mapper.map(enumerator, EnumeratorResponseDto.class);
        return enumeratorResponseDto;
    }


    public EnumeratorResponseDto getEnumeratorWithUserId(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                "Requested user Id does not exist!"));
        Enumerator enumerator = repository.findEnumeratorByUserId(userId);
        if(Objects.isNull(enumerator)) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                    "Requested enumerator object does not exist");
        }
        Enumerator enumeratorResponse = setTransientFields(enumerator);
        return mapper.map(enumeratorResponse, EnumeratorResponseDto.class);
    }


    public Page<Enumerator> findAll(String name, PageRequest pageRequest ){
        Page<Enumerator> enumeratorProperties = repository.findEnumeratorsProperties(name,pageRequest);
        if(enumeratorProperties == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        enumeratorProperties.getContent().forEach(enumerator ->{
            setTransientFields(enumerator);
        });
        return enumeratorProperties;

    }



    public void enableDisEnable (EnableDisableDto request, HttpServletRequest request1){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Enumerator enumeratorProperties = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested enumerator properties Id does not exist!"));
        enumeratorProperties.setIsActive(request.getIsActive());
        enumeratorProperties.setUpdatedBy(userCurrent.getId());

        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Disable/Enable enumerator by :" + userCurrent.getUsername() ,
                        AuditTrailFlag.UPDATE,
                        " Disable/Enable enumerator Request for:" +  enumeratorProperties.getId() ,1, Utility.getClientIp(request1));
        repository.save(enumeratorProperties);

    }


    public List<Enumerator> getAll(Boolean isActive){
        List<Enumerator> enumeratorProperties = repository.findEnumeratorByIsActive(isActive);
        for (Enumerator part : enumeratorProperties
        ) {
           setTransientFields(part);
        }
        return enumeratorProperties;

    }

    public Page<Enumerator> getAll(Boolean isActive, Pageable pageable){
        Page<Enumerator> enumeratorPropertyPage = repository.findByIsActive(isActive, pageable);
        for (Enumerator part : enumeratorPropertyPage.getContent()
        ) {
            setTransientFields(part);
        }
        return enumeratorPropertyPage;
    }

    public HashMap<String, Integer> enumeratorSummary(long enumeratorId) {
        repository.findById(enumeratorId)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested enumerator properties Id does not exist!"));

        int activeProjects = projectEnumeratorService.getEnumeratorProject(enumeratorId).size();
        int submittedSurveys = submissionService.getSurveysForProjectEnumerator(projectEnumeratorService.getEnumeratorProject(enumeratorId),
                Status.COMPLETED);
        int assignedTasks = 0;
        int pendindTasks = 0;
        int taskInProgress = 0;
        int inCompleteSurveys = 0;
        int activeLocations = 0;
        int amountEarned = 0;

        return new HashMap<String, Integer>(){{
            put("activeProjects", activeProjects);
            put("submittedSurveys", submittedSurveys);
            put("assignedTasks", assignedTasks);
            put("pendindTasks", pendindTasks);
            put("taskInProgress", taskInProgress);
            put("inCompleteSurveys", inCompleteSurveys);
            put("activeLocations", activeLocations);
            put("amountEarned", amountEarned);
        }};

    }

    public HashMap<String, Integer> enumeratorSummary(long enumeratorId, String startDate, String endDate) {
        DateFormatter.checkStartAndEndDate(startDate, endDate);
        repository.findById(enumeratorId)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested enumerator properties Id does not exist!"));

        LocalDateTime start = DateFormatter.convertToLocalDate(startDate);
        LocalDateTime end = Objects.nonNull(endDate) ? DateFormatter.convertToLocalDate(endDate) : LocalDateTime.now();

        int activeProjects = projectEnumeratorService.getEnumeratorProjectWithDate(enumeratorId, start, end).size();
        int submittedSurveys = submissionService.getSurveysForProjectEnumerator(projectEnumeratorService.getEnumeratorProjectWithDate(enumeratorId, start, end),
                Status.COMPLETED);
        int assignedTasks = 0;
        int pendindTasks = 0;
        int taskInProgress = 0;
        int inCompleteSurveys = 0;
        int activeLocations = 0;
        int amountEarned = 0;

        return new HashMap<String, Integer>() {{
            put("activeProjects", activeProjects);
            put("submittedSurveys", submittedSurveys);
            put("assignedTasks", assignedTasks);
            put("pendindTasks", pendindTasks);
            put("taskInProgress", taskInProgress);
            put("inCompleteSurveys", inCompleteSurveys);
            put("activeLocations", activeLocations);
            put("amountEarned", amountEarned);
        }};
    }

    private Enumerator setTransientFields(Enumerator enumeratorProperties) {

        if(Objects.nonNull(enumeratorProperties.getOrganisationTypeId()) && enumeratorProperties.getOrganisationTypeId() > 0)
            enumeratorProperties.setOrganisationType(organisationTypeRepository.findOrganisationTypeById(enumeratorProperties.getOrganisationTypeId()).getName());

        LGA lga = null;
        if(Objects.nonNull(enumeratorProperties.getLgaId()) && enumeratorProperties.getLgaId() > 0) {
            lga = lgaRepository.findLGAById(enumeratorProperties.getLgaId());
            enumeratorProperties.setLga(lga.getName());
        }

        if(Objects.nonNull(lga))
            enumeratorProperties.setState(stateRepository.getOne(lga.getStateId()).getName());

        if(Objects.nonNull(enumeratorProperties.getCountryId()) && enumeratorProperties.getCountryId() > 0)
            enumeratorProperties.setCountry(countryRepository.getOne(enumeratorProperties.getCountryId()).getName());

        if(Objects.nonNull(enumeratorProperties.getProjectRoleId()) && enumeratorProperties.getProjectRoleId() > 0)
            enumeratorProperties.setProjectRole(projectRoleRepository.getOne(enumeratorProperties.getProjectRoleId()).getName());

        return enumeratorProperties;
    }

    public void updateVerificationStatus(Long id, String verificationStatus) {
        Enumerator enumerator = repository.findById(id).orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                "Requested enumerator Id does not exist!"));
        validateEnumeratorVerificationStatus(verificationStatus);
        enumerator.setVerificationStatus(verificationStatus);
        repository.save(enumerator);
    }

    public List<Enumerator> getEnumeratorByVerificartionStatus(String verificationStatus) {
        validateEnumeratorVerificationStatus(verificationStatus);
        List<Enumerator> enumerators = repository.findEnumeratorByVerificationStatus(verificationStatus);
        for (Enumerator enumerator : enumerators
        ) {
            setTransientFields(enumerator);
        }
        return enumerators;
    }



    private void validateEnumeratorVerificationStatus(String verificationStatus) {
        if (!EnumUtils.isValidEnum(EnumeratorVerificationStatus.class, verificationStatus.toUpperCase()))
            throw new BadRequestException(CustomResponseCode.NOT_FOUND_EXCEPTION, "Enter a valid value for verificationStatus: PENDING/VERIFIED/UNVERIFIED");
    }
}
