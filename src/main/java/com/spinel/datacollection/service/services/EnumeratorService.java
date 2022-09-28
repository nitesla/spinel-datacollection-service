package com.spinel.datacollection.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.spinel.datacollection.core.dto.request.*;
import com.spinel.datacollection.core.dto.response.*;
import com.spinel.datacollection.core.enums.*;
import com.spinel.datacollection.core.models.Enumerator;
import com.spinel.datacollection.core.models.LGA;
import com.spinel.datacollection.service.helper.*;
import com.spinel.datacollection.service.repositories.*;
import com.spinel.framework.dto.requestDto.ActivateUserAccountDto;
import com.spinel.framework.dto.requestDto.ChangePasswordDto;
import com.spinel.framework.dto.responseDto.ActivateUserResponse;
import com.spinel.framework.exceptions.BadRequestException;
import com.spinel.framework.exceptions.ConflictException;
import com.spinel.framework.exceptions.NotFoundException;
import com.spinel.framework.models.PreviousPasswords;
import com.spinel.framework.models.User;
import com.spinel.framework.models.UserRole;
import com.spinel.framework.notification.requestDto.NotificationRequestDto;
import com.spinel.framework.notification.requestDto.RecipientRequest;
import com.spinel.framework.repositories.PreviousPasswordRepository;
import com.spinel.framework.repositories.UserRepository;
import com.spinel.framework.repositories.UserRoleRepository;
import com.spinel.framework.service.*;
import com.spinel.framework.utils.AuditTrailFlag;
import com.spinel.framework.utils.CustomResponseCode;
import com.spinel.framework.utils.Utility;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;


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
    private final ProjectEnumeratorRepository projectEnumeratorRepository;
    private final UserService userService;


    public EnumeratorService(EnumeratorRepository repository, UserRepository userRepository,
                             PreviousPasswordRepository previousPasswordRepository, ModelMapper mapper,
                             ObjectMapper objectMapper, Validations validations, NotificationService notificationService,
                             LGARepository lgaRepository, AuditTrailService auditTrailService,
                             StateRepository stateRepository, UserRoleRepository userRoleRepository, ProjectEnumeratorService projectEnumeratorService,
                             CountryRepository countryRepository, ProjectRoleRepository projectRoleRepository, SubmissionService submissionService, ProjectEnumeratorRepository projectEnumeratorRepository, UserService userService) {
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
        this.projectEnumeratorRepository = projectEnumeratorRepository;
        this.userService = userService;
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
                        .userBankId(enumeratorExist.getUserBankId())
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
        saveEnumerator.setVerification(VerificationStatus.one);
        saveEnumerator.setUserBankId(request.getUserBankId());
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
                .userBankId(enumeratorResponse.getUserBankId())
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
//        notificationRequestDto.setRecipient(String.valueOf(recipient));
        notificationRequestDto.setRecipient(emailRecipient.getEmail());
        notificationService.emailNotificationRequest(notificationRequestDto);

//        SmsRequest smsRequest = SmsRequest.builder()
//                .message("Activation Otp " + " " + user.getResetToken())
//                .phoneNumber(emailRecipient.getPhone())
//                .build();
//        notificationService.smsNotificationRequest(smsRequest);


//        WhatsAppRequest whatsAppRequest = WhatsAppRequest.builder()
//                .message("Activation Otp " + " " + user.getResetToken())
//                .phoneNumber(emailRecipient.getPhone())
//                .build();
//        whatsAppService.whatsAppNotification(whatsAppRequest);

        auditTrailService
                .logEvent(response.getUsername(),
                        "SignUp Enumerator :" + response.getUsername(),
                        AuditTrailFlag.SIGNUP,
                        " Sign up Enumerator Request for:" + user.getFirstName() + " " + user.getLastName() + " " + user.getEmail()
                        , 1, Utility.getClientIp(request1));
        return response;
    }

    public CompleteSignUpResponse completeSignUp(CompleteSignupRequest request, HttpServletRequest request1) {
        request.setVerificationStatus(String.valueOf(EnumeratorVerificationStatus.VERIFIED));
        validations.validateEnumeratorProperties(request);
        Enumerator enumerator = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Enumerator Id does not exist!"));
        mapper.map(request, enumerator);

        enumerator.setUpdatedBy(enumerator.getUserId());
        enumerator.setIsActive(true);
        enumerator.setStatus(EnumeratorStatus.ACTIVE);
        enumerator.setVerification(VerificationStatus.three);
        enumerator.setIdCard(request.getIdCard());
        enumerator.setIdNumber(request.getIdNumber());
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
                .idCard(enumerator.getIdCard())
                .idNumber(enumerator.getIdNumber())
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
                SubmissionStatus.ACCEPTED);
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
        repository.findById(enumeratorId)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested enumerator properties Id does not exist!"));

        LocalDateTime start = DateFormatter.convertToLocalDate(startDate);
        LocalDateTime end = Objects.nonNull(endDate) ? DateFormatter.convertToLocalDate(endDate) : LocalDateTime.now();
        DateFormatter.checkStartAndEndDate(start, end);

        int activeProjects = projectEnumeratorService.getEnumeratorProjectWithDate(enumeratorId, start, end).size();
        int submittedSurveys = submissionService.getSurveysForProjectEnumerator(projectEnumeratorService.getEnumeratorProjectWithDate(enumeratorId, start, end),
                SubmissionStatus.ACCEPTED);
        int assignedTasks = 0;
        int pendindTasks = 0;
        int taskInProgress = 0;
        int inCompleteSurveys = submissionService.getSurveysForProjectEnumerator(projectEnumeratorService.getEnumeratorProjectWithDate(enumeratorId, start, end),
                SubmissionStatus.INREVIEW);;
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

    private HashMap<String, Integer> enumeratorProjectSummary(long enumeratorId, LocalDateTime startDate, LocalDateTime endDate) {
        repository.findById(enumeratorId)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested enumerator properties Id does not exist!"));

        int totalProjects = projectEnumeratorService.getEnumeratorProjectWithDate(enumeratorId, startDate, endDate).size();
        int projectsInProgress = projectEnumeratorRepository.findProjectEnumeratorsWithProjectStatus(enumeratorId, Status.ONGOING.toString(), startDate, endDate).size();
        int pendingProjects = projectEnumeratorRepository.findProjectEnumeratorsWithProjectStatus(enumeratorId, Status.DRAFT.toString(), startDate, endDate).size();
        int totalSurvey = submissionService.getSurveysForProjectEnumerator(projectEnumeratorService.getEnumeratorProjectWithDate(enumeratorId, startDate, endDate), null);

        return new HashMap<String, Integer>() {{
            put("totalProjects", totalProjects);
            put("projectsInProgress", projectsInProgress);
            put("pendingProjects", pendingProjects);
            put("totalSurvey", totalSurvey);
        }};
    }


    public HashMap<String, Integer> enumeratorProjectSummary(Long enumeratorId, int length, String dateType) {
        DateEnum.validateDateEnum(dateType);
        LocalDateTime startDate = LocalDateTime.now();
        HashMap<String, Integer> submissions = null;

        if(DateEnum.MONTH.getValue().equals(dateType)) {
            LocalDateTime endDate = startDate.minusMonths(length);
            submissions = enumeratorProjectSummary(enumeratorId, startDate, endDate);
        }
        if(DateEnum.WEEK.getValue().equals(dateType)) {
            LocalDateTime endDate = startDate.minusDays(length*7);
            submissions = enumeratorProjectSummary(enumeratorId, startDate, endDate);
        }
        if(DateEnum.DAY.getValue().equals(dateType)) {
            LocalDateTime endDate = startDate.minusDays(length);
            submissions = enumeratorProjectSummary(enumeratorId, startDate, endDate);
        }
        return submissions;
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

    public EnumeratorKYCResponseDto getEnumeratorKYC(Long id) {
//        userRepository.findById(userId)
//                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
//                        "Requested user Id does not exist!"));
        Enumerator enumerator = repository.findEnumeratorById(id);
        if(Objects.isNull(enumerator)) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                    "Requested enumerator object does not exist");
        }
        EnumeratorKYCResponseDto enumeratorResponse = new EnumeratorKYCResponseDto();
        enumeratorResponse.setAddress(enumerator.getAddress());
        enumeratorResponse.setFirstName(enumerator.getFirstName());
        enumeratorResponse.setLastName(enumerator.getLastName());
        enumeratorResponse.setVerificationStatus(enumerator.getVerificationStatus());
        enumeratorResponse.setEmail(enumerator.getEmail());
        enumeratorResponse.setCardImage(enumerator.getIdCard());
        enumeratorResponse.setIdCardNumber(enumerator.getIdNumber());
        enumeratorResponse.setGender(enumerator.getGender());
        enumeratorResponse.setPhone(enumerator.getPhone());
        enumeratorResponse.setPictureUrl(enumerator.getPictureUrl());
        return mapper.map(enumeratorResponse, EnumeratorKYCResponseDto.class);
    }

    public ActivateUserResponse validateOtpAndActivateUser(ActivateUserAccountDto request) {
        ActivateUserResponse activateUserResponse = userService.activateUser(request);
        Enumerator enumerator = repository.findEnumeratorByUserId(activateUserResponse.getUserId());
        enumerator.setIsActive(true);
        enumerator.setUpdatedDate(LocalDateTime.now());
        repository.save(enumerator);
        return activateUserResponse;
    }

    public Page<Enumerator> findPaginated(GetRequestDto request) {
        GenericSpecification<Enumerator> genericSpecification = new GenericSpecification<Enumerator>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        DateTimeFormatter formatter1 = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.systemDefault());
        SimpleDateFormat enUsFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        SimpleDateFormat localFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());



        request.getFilterCriteria().forEach(filter-> {
            if (filter.getFilterParameter() != null) {
                if (filter.getFilterParameter().equalsIgnoreCase("firstName")) {
                    genericSpecification.add(new SearchCriteria("firstName", filter.getFilterValue(), SearchOperation.MATCH));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("lastName")) {
                    genericSpecification.add(new SearchCriteria("lastName", filter.getFilterValue(), SearchOperation.MATCH));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("referralCode")) {
                    genericSpecification.add(new SearchCriteria("referralCode", filter.getFilterValue(), SearchOperation.MATCH));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("status")) {
                    genericSpecification.add(new SearchCriteria("status", filter.getFilterValue(), SearchOperation.MATCH));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("cac")) {
                    genericSpecification.add(new SearchCriteria("cac", filter.getFilterValue(), SearchOperation.MATCH));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("address")) {
                    genericSpecification.add(new SearchCriteria("address", filter.getFilterValue(), SearchOperation.MATCH));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("refereeCode")) {
                    genericSpecification.add(new SearchCriteria("refereeCode", filter.getFilterValue(), SearchOperation.MATCH));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("corporateName")) {
                    genericSpecification.add(new SearchCriteria("corporateName", filter.getFilterValue(), SearchOperation.MATCH));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("phone")) {
                    genericSpecification.add(new SearchCriteria("phone", filter.getFilterValue(), SearchOperation.MATCH));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("email")) {
                    genericSpecification.add(new SearchCriteria("email", filter.getFilterValue(), SearchOperation.MATCH));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("gender")) {
                    genericSpecification.add(new SearchCriteria("gender", filter.getFilterValue(), SearchOperation.MATCH));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("efficiency")) {
                    genericSpecification.add(new SearchCriteria("efficiency", filter.getFilterValue(), SearchOperation.MATCH));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("BVN")) {
                    genericSpecification.add(new SearchCriteria("BVN", filter.getFilterValue(), SearchOperation.MATCH));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("idType")) {
                    genericSpecification.add(new SearchCriteria("idType", filter.getFilterValue(), SearchOperation.MATCH));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("frontOfId")) {
                    genericSpecification.add(new SearchCriteria("frontOfId", filter.getFilterValue(), SearchOperation.EQUAL));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("backOfId")) {
                    genericSpecification.add(new SearchCriteria("backOfId", filter.getFilterValue(), SearchOperation.EQUAL));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("verificationStatus")) {
                    genericSpecification.add(new SearchCriteria("verificationStatus", filter.getFilterValue(), SearchOperation.MATCH));
                }

                if (filter.getFilterParameter().equalsIgnoreCase("verification")) {
                    genericSpecification.add(new SearchCriteria("verification", filter.getFilterValue(), SearchOperation.MATCH));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("idCard")) {
                    genericSpecification.add(new SearchCriteria("idCard", filter.getFilterValue(), SearchOperation.MATCH));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("idNumber")) {
                    genericSpecification.add(new SearchCriteria("idNumber", filter.getFilterValue(), SearchOperation.MATCH));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("verificationStatus")) {
                    genericSpecification.add(new SearchCriteria("verificationStatus", filter.getFilterValue(), SearchOperation.MATCH));
                }


                if (filter.getFilterParameter().equalsIgnoreCase("isActive")) {
                    genericSpecification.add(new SearchCriteria("isActive", Boolean.valueOf(filter.getFilterValue()), SearchOperation.EQUAL));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("isCorp")) {
                    genericSpecification.add(new SearchCriteria("isCorp", Boolean.valueOf(filter.getFilterValue()), SearchOperation.EQUAL));
                }

                if (filter.getFilterParameter().equalsIgnoreCase("userId")) {
                    genericSpecification.add(new SearchCriteria("userId", filter.getFilterValue(), SearchOperation.EQUAL));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("projectRoleId")) {
                    genericSpecification.add(new SearchCriteria("projectRoleId", filter.getFilterValue(), SearchOperation.EQUAL));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("lgaId")) {
                    genericSpecification.add(new SearchCriteria("lgaId", filter.getFilterValue(), SearchOperation.EQUAL));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("countryId")) {
                    genericSpecification.add(new SearchCriteria("countryId", filter.getFilterValue(), SearchOperation.EQUAL));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("userBankId")) {
                    genericSpecification.add(new SearchCriteria("userBankId", filter.getFilterValue(), SearchOperation.EQUAL));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("organisationTypeId")) {
                    genericSpecification.add(new SearchCriteria("organisationTypeId", filter.getFilterValue(), SearchOperation.EQUAL));
                }

            }
        });

//        request.getFilterDate().forEach(filter-> {
//            if (filter.getDateParameter() != null && filter.getDateParameter().equalsIgnoreCase("createdDate")) {
//                if (filter.getFromDate() != null) {
//                    if (filter.getToDate() != null && filter.getFromDate().isAfter(filter.getToDate()))
//                        throw new BadRequestException(CustomResponseCode.BAD_REQUEST,"fromDate can't be greater than toDate");
//                    LocalDateTime fromDate = LocalDateTime.from((filter.getFromDate().atZone(ZoneId.systemDefault()).toInstant()));
//                    genericSpecification.add(new SearchCriteria("createdDate", fromDate, SearchOperation.GREATER_THAN_EQUAL));
//
//                }
//
//                if (filter.getToDate() != null) {
//                    if (filter.getFromDate() == null)
//                        throw new BadRequestException(CustomResponseCode.BAD_REQUEST,"'fromDate' must be included along with 'toDate' in the request");
//                    LocalDateTime toDate = LocalDateTime.from(filter.getToDate().atZone(ZoneId.systemDefault()).toInstant());
//                    genericSpecification.add(new SearchCriteria("createdDate", toDate, SearchOperation.LESS_THAN_EQUAL));
//
//                }
//            }
//        });


        Sort sortType = (request.getSortDirection() != null && request.getSortDirection().equalsIgnoreCase("asc"))
                ?  Sort.by(Sort.Order.asc(request.getSortBy())) :   Sort.by(Sort.Order.desc(request.getSortBy()));

        PageRequest pageRequest = PageRequest.of(request.getPage(), request.getPageSize(), sortType);

        return repository.findAll(genericSpecification, pageRequest);


    }

    public List<Enumerator> findList(GetRequestDto request) {
        GenericSpecification<Enumerator> genericSpecification = new GenericSpecification<Enumerator>();

        request.getFilterCriteria().forEach(filter-> {
            if (filter.getFilterParameter() != null) {
                if (filter.getFilterParameter().equalsIgnoreCase("firstName")) {
                    genericSpecification.add(new SearchCriteria("firstName", filter.getFilterValue(), SearchOperation.MATCH));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("lastName")) {
                    genericSpecification.add(new SearchCriteria("lastName", filter.getFilterValue(), SearchOperation.MATCH));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("referralCode")) {
                    genericSpecification.add(new SearchCriteria("referralCode", filter.getFilterValue(), SearchOperation.MATCH));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("status")) {
                    genericSpecification.add(new SearchCriteria("status", filter.getFilterValue(), SearchOperation.MATCH));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("cac")) {
                    genericSpecification.add(new SearchCriteria("cac", filter.getFilterValue(), SearchOperation.MATCH));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("address")) {
                    genericSpecification.add(new SearchCriteria("address", filter.getFilterValue(), SearchOperation.MATCH));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("refereeCode")) {
                    genericSpecification.add(new SearchCriteria("refereeCode", filter.getFilterValue(), SearchOperation.MATCH));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("corporateName")) {
                    genericSpecification.add(new SearchCriteria("corporateName", filter.getFilterValue(), SearchOperation.MATCH));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("phone")) {
                    genericSpecification.add(new SearchCriteria("phone", filter.getFilterValue(), SearchOperation.MATCH));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("email")) {
                    genericSpecification.add(new SearchCriteria("email", filter.getFilterValue(), SearchOperation.MATCH));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("gender")) {
                    genericSpecification.add(new SearchCriteria("gender", filter.getFilterValue(), SearchOperation.MATCH));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("efficiency")) {
                    genericSpecification.add(new SearchCriteria("efficiency", filter.getFilterValue(), SearchOperation.MATCH));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("BVN")) {
                    genericSpecification.add(new SearchCriteria("BVN", filter.getFilterValue(), SearchOperation.MATCH));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("idType")) {
                    genericSpecification.add(new SearchCriteria("idType", filter.getFilterValue(), SearchOperation.MATCH));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("frontOfId")) {
                    genericSpecification.add(new SearchCriteria("frontOfId", filter.getFilterValue(), SearchOperation.EQUAL));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("backOfId")) {
                    genericSpecification.add(new SearchCriteria("backOfId", filter.getFilterValue(), SearchOperation.EQUAL));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("verificationStatus")) {
                    genericSpecification.add(new SearchCriteria("verificationStatus", filter.getFilterValue(), SearchOperation.MATCH));
                }

                if (filter.getFilterParameter().equalsIgnoreCase("verification")) {
                    genericSpecification.add(new SearchCriteria("verification", filter.getFilterValue(), SearchOperation.MATCH));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("idCard")) {
                    genericSpecification.add(new SearchCriteria("idCard", filter.getFilterValue(), SearchOperation.MATCH));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("idNumber")) {
                    genericSpecification.add(new SearchCriteria("idNumber", filter.getFilterValue(), SearchOperation.MATCH));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("verificationStatus")) {
                    genericSpecification.add(new SearchCriteria("verificationStatus", filter.getFilterValue(), SearchOperation.MATCH));
                }


                if (filter.getFilterParameter().equalsIgnoreCase("isActive")) {
                    genericSpecification.add(new SearchCriteria("isActive", Boolean.valueOf(filter.getFilterValue()), SearchOperation.EQUAL));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("isCorp")) {
                    genericSpecification.add(new SearchCriteria("isCorp", Boolean.valueOf(filter.getFilterValue()), SearchOperation.EQUAL));
                }

                if (filter.getFilterParameter().equalsIgnoreCase("userId")) {
                    genericSpecification.add(new SearchCriteria("userId", filter.getFilterValue(), SearchOperation.EQUAL));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("projectRoleId")) {
                    genericSpecification.add(new SearchCriteria("projectRoleId", filter.getFilterValue(), SearchOperation.EQUAL));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("lgaId")) {
                    genericSpecification.add(new SearchCriteria("lgaId", filter.getFilterValue(), SearchOperation.EQUAL));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("countryId")) {
                    genericSpecification.add(new SearchCriteria("countryId", filter.getFilterValue(), SearchOperation.EQUAL));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("userBankId")) {
                    genericSpecification.add(new SearchCriteria("userBankId", filter.getFilterValue(), SearchOperation.EQUAL));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("organisationTypeId")) {
                    genericSpecification.add(new SearchCriteria("organisationTypeId", filter.getFilterValue(), SearchOperation.EQUAL));
                }
            }
        });

//        request.getFilterDate().forEach(filter-> {
//            if (filter.getDateParameter() != null && filter.getDateParameter().equalsIgnoreCase("createdDate")) {
//                if (filter.getFromDate() != null) {
//                    if (filter.getToDate() != null && filter.getFromDate().isAfter(filter.getToDate()))
//                        throw new BadRequestException(CustomResponseCode.BAD_REQUEST,"fromDate can't be greater than toDate");
//                    genericSpecification.add(new SearchCriteria("createdDate", filter.getFromDate(), SearchOperation.GREATER_THAN_EQUAL));
//                }
//
//                if (filter.getToDate() != null) {
//                    if (filter.getFromDate() == null)
//                        throw new BadRequestException(CustomResponseCode.BAD_REQUEST,"'fromDate' must be included along with 'toDate' in the request");
//                    genericSpecification.add(new SearchCriteria("createdDate", filter.getToDate(), SearchOperation.LESS_THAN_EQUAL));
//                }
//            }
//        });


        Sort sortType = (request.getSortDirection() != null && request.getSortDirection().equalsIgnoreCase("asc"))
                ?  Sort.by(Sort.Order.asc(request.getSortBy())) :   Sort.by(Sort.Order.desc(request.getSortBy()));

        return repository.findAll(genericSpecification, sortType);


    }

    public Page<Enumerator> getEntities(GetRequestDto request) {
        Sort sortType = (request.getSortDirection() != null && request.getSortDirection().equalsIgnoreCase("asc"))
                ?  Sort.by(Sort.Order.asc(request.getSortBy())) :   Sort.by(Sort.Order.desc(request.getSortBy()));
        PageRequest pageRequest = PageRequest.of(request.getPage(), request.getPageSize(), sortType);
        return repository.findAll(pageRequest);
    }

}
