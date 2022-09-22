package com.spinel.datacollection.service.services;

import com.sabi.datacollection.core.dto.response.*;
import com.spinel.datacollection.core.dto.request.*;
import com.spinel.datacollection.core.dto.response.CompleteProjectOwnerSignUpResponse;
import com.spinel.datacollection.core.dto.response.ProjectOwnerActivationResponse;
import com.spinel.datacollection.core.dto.response.ProjectOwnerResponseDto;
import com.spinel.datacollection.core.dto.response.ProjectOwnerSignUpResponseDto;
import com.spinel.datacollection.core.dto.wallet.CreateWalletDto;
import com.spinel.datacollection.core.enums.Status;
import com.spinel.datacollection.core.enums.SubmissionStatus;
import com.spinel.datacollection.core.enums.UserCategory;
import com.spinel.datacollection.core.models.*;
import com.spinel.datacollection.service.helper.DateFormatter;
import com.spinel.datacollection.service.helper.Validations;


import com.spinel.datacollection.service.repositories.*;
import com.spinel.framework.dto.requestDto.ActivateUserAccountDto;
import com.spinel.framework.dto.requestDto.ChangePasswordDto;
import com.spinel.framework.dto.requestDto.ForgetPasswordDto;
import com.spinel.framework.dto.requestDto.PasswordActivationRequest;
import com.spinel.framework.dto.responseDto.ActivateUserResponse;
import com.spinel.framework.dto.responseDto.UserActivationResponse;
import com.spinel.framework.exceptions.BadRequestException;
import com.spinel.framework.exceptions.ConflictException;
import com.spinel.framework.exceptions.NotFoundException;
import com.spinel.framework.models.PreviousPasswords;
import com.spinel.framework.models.User;
import com.spinel.framework.models.UserRole;
import com.spinel.framework.notification.requestDto.*;
import com.spinel.framework.repositories.PreviousPasswordRepository;
import com.spinel.framework.repositories.UserRepository;
import com.spinel.framework.repositories.UserRoleRepository;
import com.spinel.framework.service.*;
import com.spinel.framework.utils.AuditTrailFlag;
import com.spinel.framework.utils.CustomResponseCode;
import com.spinel.framework.utils.Utility;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;

@SuppressWarnings("ALL")
@Slf4j
@Service
public class ProjectOwnerService {


    @Autowired
    private NotificationService notificationService;

    @Autowired
    private WhatsAppService whatsAppService;


    private final ProjectOwnerUserRepository projectOwnerUserRepository;
    private final SectorRepository sectorRepository;
    private final LGARepository lgaRepository;
    private final AuditTrailService auditTrailService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final PreviousPasswordRepository previousPasswordRepository;
    private final ProjectOwnerRepository projectOwnerRepository;
    private final ModelMapper mapper;
    private final Validations validations;
    private final UserRoleRepository userRoleRepository;
    private final ProjectRepository projectRepository;
    private final UserService userService;
    private final SubmissionService submissionService;
    private final DataWalletService dataWalletService;

    public ProjectOwnerService(ProjectOwnerUserRepository projectOwnerUserRepository, SectorRepository sectorRepository, LGARepository lgaRepository,
                               AuditTrailService auditTrailService, PasswordEncoder passwordEncoder, UserRepository userRepository, PreviousPasswordRepository previousPasswordRepository,
                               ProjectOwnerRepository projectOwnerRepository, ModelMapper mapper, Validations validations, UserRoleRepository userRoleRepository, ProjectRepository projectRepository, UserService userService, SubmissionService submissionService, DataWalletService dataWalletService) {
        this.projectOwnerUserRepository = projectOwnerUserRepository;
        this.sectorRepository = sectorRepository;
        this.lgaRepository = lgaRepository;
        this.auditTrailService = auditTrailService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.previousPasswordRepository = previousPasswordRepository;
        this.projectOwnerRepository = projectOwnerRepository;
        this.mapper = mapper;
        this.validations = validations;
        this.userRoleRepository = userRoleRepository;
        this.projectRepository = projectRepository;
        this.userService = userService;
        this.submissionService = submissionService;
        this.dataWalletService = dataWalletService;
    }

    public ProjectOwnerSignUpResponseDto projectOwnerSignUp(ProjectOwnerSignUpDto request, HttpServletRequest request1) {
        validations.validateProjectOwnerSignUp(request);
        User user = mapper.map(request, User.class);
        User userExists = userRepository.findByEmailOrPhone(request.getEmail(), request.getPhone());
        if(userExists != null && userExists.getPasswordChangedOn() == null) {
            ProjectOwner projectOwnerExists = projectOwnerRepository.findByUserId(userExists.getId());
            if (projectOwnerExists != null) {
                ProjectOwnerSignUpResponseDto projectOwnerSignUpResponseDto = ProjectOwnerSignUpResponseDto.builder()
                        .id(userExists.getId())
                        .email(userExists.getEmail())
                        .firstName(userExists.getFirstName())
                        .lastName(userExists.getLastName())
                        .phone(userExists.getPhone())
                        .username(userExists.getUsername())
                        .sectorId(projectOwnerExists.getSectorId())
                        .corporateName(projectOwnerExists.getCorporateName())
                        .userBankId(projectOwnerExists.getUserBankId())
                        .build();
                return projectOwnerSignUpResponseDto;
            } else {
                throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Project Owner Id does not  match");
            }
        } else if (userExists != null && userExists.getPasswordChangedOn() != null) {
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "Project Owner already exists");
        }
        String password = request.getPassword();
        user.setPassword(passwordEncoder.encode(password));
        user.setUserCategory(UserCategory.PROJECT_OWNER.toString());
        user.setUsername(request.getEmail());
        user.setLoginAttempts(0);
        user.setResetToken(Utility.registrationCode("HHmmss"));
        user.setResetTokenExpirationDate(Utility.tokenExpiration());
        user.setCreatedBy(0l);
        user.setIsActive(false);
        user = userRepository.save(user);
        log.info("Created new project owner user - {}", user);

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

        ProjectOwner saveProjectOwner = new ProjectOwner();
        saveProjectOwner.setFirstname(request.getFirstName());
        saveProjectOwner.setLastname(request.getLastName());
        saveProjectOwner.setUserId(user.getId());
        saveProjectOwner.setIsActive(false);
        saveProjectOwner.setCreatedBy(user.getId());
        saveProjectOwner.setIsCorp(request.getIsCorp());
        saveProjectOwner.setUserBankId(request.getUserBankId());
        if(request.getIsCorp() == true) {
            saveProjectOwner.setCorporateName(request.getCorporateName());
        }

        ProjectOwner projectOwner = projectOwnerRepository.save(saveProjectOwner);
        log.info("Created new Project Owner - {}", saveProjectOwner);

        ProjectOwnerUser projectOwnerUser = new ProjectOwnerUser();
        projectOwnerUser.setProjectOwnerId(projectOwner.getId());
        projectOwnerUser.setUserId(user.getId());
        projectOwnerUserRepository.save(projectOwnerUser);
        createProjectOwnerWallet(user.getId());

        ProjectOwnerSignUpResponseDto response = ProjectOwnerSignUpResponseDto.builder()
                .id(user.getId())
                .sectorId(request.getSectorId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .corporateName(projectOwner.getCorporateName())
                .userBankId(projectOwner.getUserBankId())
                .username(user.getUsername())
                .build();


        // --------  sending token  -----------

        NotificationRequestDto notificationRequestDto = new NotificationRequestDto();
        User emailRecipient = userRepository.getOne(user.getId());
        notificationRequestDto.setMessage("Activation Otp " + " " + user.getResetToken());
        List<RecipientRequest> recipient = new ArrayList<>();
        recipient.add(RecipientRequest.builder()
                .email(emailRecipient.getEmail())
                .build());
        notificationRequestDto.setRecipient(String.valueOf(recipient));
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
        VoiceOtpRequest voiceOtpRequest = VoiceOtpRequest.builder()
                .message("Activation Otp is " + " " + user.getResetToken())
                .phoneNumber(emailRecipient.getPhone())
                .build();
        notificationService.voiceOtp(voiceOtpRequest);
        auditTrailService
                .logEvent(response.getUsername(),
                        "SignUp Project Owner :" + response.getUsername(),
                        AuditTrailFlag.SIGNUP,
                        " SignUp Project Owner Request for:" + user.getFirstName() + " " + user.getLastName()
                        , 1, Utility.getClientIp(request1));
          return response;
    }

    public CompleteProjectOwnerSignUpResponse completeSignUp(CompleteSignupProjectOwnerRequest request){
       validations.validateProjectOwnerCompleteSignUp(request);
       ProjectOwner projectOwner = projectOwnerRepository.findById(request.getId())
               .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                       "Requested Project Owner does not exist!"));
       mapper.map(request, projectOwner);

       if(projectOwner.getIsActive())
           throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Project owner is already active. Double check project Id");

       projectOwner.setUpdatedBy(projectOwner.getUserId());
       projectOwner.setIsActive(true);
       projectOwnerRepository.save(projectOwner);
       log.info("complete sign up - {}", projectOwner);

       User user = userRepository.getOne(projectOwner.getUserId());
       user.setIsActive(true);
       user.setUpdatedBy(projectOwner.getUserId());
       user.setPasswordChangedOn(LocalDateTime.now());
       userRepository.save(user);

        CompleteProjectOwnerSignUpResponse response = CompleteProjectOwnerSignUpResponse.builder()
                .projectOwnerId(projectOwner.getId())
                .email(projectOwner.getEmail())
                .corporateName(projectOwner.getCorporateName())
                .phone(projectOwner.getPhone())
                .address(projectOwner.getAddress())
                .userId(projectOwner.getUserId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .userEmail(user.getEmail())
                .userName(user.getUsername())
                .userPhone(user.getPhone())
                .idCard(projectOwner.getIdCard())
                .idNumber(projectOwner.getIdNumber())
                .build();
        return response;
    }

    public ProjectOwnerActivationResponse passwordActivation(ChangePasswordDto request) {
        User user = userRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested user id does not exist!"));

        String password = request.getPassword();
        user.setPassword(passwordEncoder.encode(password));
        user.setPasswordChangedOn(LocalDateTime.now());
        user = userRepository.save(user);

        PreviousPasswords previousPasswords = PreviousPasswords.builder()
                .userId(user.getId())
                .password(user.getPassword())
                .createdDate(LocalDateTime.now())
                .build();
        previousPasswordRepository.save(previousPasswords);

        ProjectOwner projectOwner = projectOwnerRepository.findByUserId(user.getId());

        ProjectOwnerActivationResponse response = ProjectOwnerActivationResponse.builder()
                .userId(user.getId())
                .projectOwnerId(projectOwner.getId())
                .phone(user.getPhone())
                .email(user.getEmail())
                .build();

        return response;
    }

    public ProjectOwnerResponseDto createProjectOwner(ProjectOwnerDto request, HttpServletRequest request1) {
        validations.validateCreateProjectOwner(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        User user = mapper.map(request, User.class);
        User userExists = userRepository.findByEmailOrPhone(request.getEmail(), request.getPhone());
        if(userExists != null && userExists.getPasswordChangedOn() == null) {
            ProjectOwner projectOwnerExists = projectOwnerRepository.findByUserId(userExists.getId());
            if (projectOwnerExists != null) {
                ProjectOwnerResponseDto projectOwnerResponseDto = ProjectOwnerResponseDto.builder()
                        .id(userExists.getId())
                        .email(userExists.getEmail())
                        .firstname(userExists.getFirstName())
                        .lastname(userExists.getLastName())
                        .phone(userExists.getPhone())
                        .corporateName(projectOwnerExists.getCorporateName())
                        .build();
                return projectOwnerResponseDto;
            } else {
                throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Project Owner Id does not  match");
            }
        } else if (userExists != null && userExists.getPasswordChangedOn() != null) {
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "Project Owner already exists");
        }
        ProjectOwner projectOwner = mapper.map(request, ProjectOwner.class);
        ProjectOwner projectOwnerExists = projectOwnerRepository.findProjectOwnerById(request.getId());
        if (projectOwnerExists != null) {
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "Project owner already exists");
        }

        user.setUserCategory(UserCategory.PROJECT_OWNER.toString());
        user.setUsername(request.getEmail());
        user.setLoginAttempts(0);
        user.setResetToken(Utility.registrationCode("HHmmss"));
        user.setResetTokenExpirationDate(Utility.tokenExpiration());
        user.setCreatedBy(0l);
        user.setIsActive(false);
        user = userRepository.save(user);
        log.info("Created new project owner user - {}", user);

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


        projectOwner.setCreatedBy(userCurrent.getId());
        projectOwner.setIsActive(false);
        projectOwner.setUserId(user.getId());
        projectOwnerRepository.save(projectOwner);
        log.info("Created new Project owner - {}", projectOwner);

        // --------  sending token  -----------

        NotificationRequestDto notificationRequestDto = new NotificationRequestDto();
        User emailRecipient = userRepository.getOne(user.getId());
        notificationRequestDto.setMessage("Activation Otp " + " " + user.getResetToken());
        List<RecipientRequest> recipient = new ArrayList<>();
        recipient.add(RecipientRequest.builder()
                .email(emailRecipient.getEmail())
                .build());
        notificationRequestDto.setRecipient(String.valueOf(recipient));
        notificationService.emailNotificationRequest(notificationRequestDto);
        auditTrailService
                .logEvent(user.getUsername(),
                        "Create Project Owner :" + user.getUsername(),
                        AuditTrailFlag.SIGNUP,
                        " SignUp Project Owner Request for:" + user.getFirstName() + " " + user.getLastName()
                        , 1, Utility.getClientIp(request1));
        return mapper.map(projectOwner, ProjectOwnerResponseDto.class);
    }

    public ProjectOwnerResponseDto updateProjectOwner(UpdateProjectOwnerDto request, HttpServletRequest request1) {
        validations.validateUpdateProjectOwner(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        ProjectOwner projectOwner = projectOwnerRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested project owner does not exist"));
        User user = userRepository.findById(projectOwner.getUserId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested user does not exist"));
        mapper.map(request, projectOwner);
        projectOwner.setUpdatedBy(userCurrent.getId());


        User userEmail = userRepository.findByEmail(request.getEmail());
        if(Objects.nonNull(userEmail) && userEmail.getId() != projectOwner.getUserId())
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "User with Email already exists");

        User userPhone = userRepository.findByPhone(request.getPhone());
        if(Objects.nonNull(userPhone) && userPhone.getId() != projectOwner.getUserId())
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "User with Phone already exists");
        user.setFirstName(request.getFirstname());
        user.setLastName(request.getLastname());
        user.setEmail(request.getEmail());
        user.setUsername(request.getEmail());
        user.setPhone(request.getPhone());
        user.setUpdatedBy(userCurrent.getId());
        userRepository.save(user);
        projectOwnerRepository.save(projectOwner);

        log.info("Project owner record updated - {}", projectOwner);
        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Update Project owner :" + userCurrent.getUsername(),
                        AuditTrailFlag.UPDATE,
                        " Update Project owner Request for:" + projectOwner.getEmail() ,1, Utility.getClientIp(request1));
        return mapper.map(projectOwner, ProjectOwnerResponseDto.class);
    }

    public ProjectOwnerResponseDto findProjectOwnerById(Long id) {
        ProjectOwner projectOwner = projectOwnerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Project Owner Id does not exist!"));
        setTransientFields(projectOwner);
        return mapper.map(projectOwner, ProjectOwnerResponseDto.class);
    }

    public Page<ProjectOwner> findAll(String firstname, String lastname, String email, Pageable pageable) {
        Page<ProjectOwner> projectOwners = projectOwnerRepository.findProjectOwners(firstname, lastname, email, pageable);
        if(projectOwners == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        projectOwners.getContent().forEach(projectOwner -> {
            setTransientFields(projectOwner);
        });
        return projectOwners;
    }

    public ProjectOwnerResponseDto findProjectOwnerByUserId(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested user Id does not exist!"));
        ProjectOwner projectOwner = projectOwnerRepository.findProjectOwnerByUserId(userId);
        if(Objects.isNull(projectOwner)) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                    "Requested project owner object does not exist");
        }
        ProjectOwner projectOwnerResponse = setTransientFields(projectOwner);
        return mapper.map(projectOwnerResponse, ProjectOwnerResponseDto.class);
    }

    public void enableDisable (EnableDisableDto request, HttpServletRequest request1){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        ProjectOwner projectOwner = projectOwnerRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Project Owner Id does not exist!"));
        projectOwner.setIsActive(request.getIsActive());
        projectOwner.setUpdatedBy(userCurrent.getId());

        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Disable/Enable project owner by :" + userCurrent.getUsername() ,
                        AuditTrailFlag.UPDATE,
                        " Disable/Enable project owner Request for:" +  projectOwner.getId() ,1, Utility.getClientIp(request1));
        projectOwnerRepository.save(projectOwner);

    }

    public List<ProjectOwner> getAll(Boolean isActive){
        List<ProjectOwner> projectOwners = projectOwnerRepository.findByIsActive(isActive);

        for ( ProjectOwner projectOwner: projectOwners){
            setTransientFields(projectOwner);
        }
        return projectOwners;
    }

    public void changeUserPassword(ChangePasswordDto request) {
        userService.changeUserPassword(request);
    }

    public Boolean getPrevPasswords(Long userId,String password){
        return userService.getPrevPasswords(userId, password);
    }

    public void forgetPassword (ForgetPasswordDto request) {
        userService.forgetPassword(request);
    }

    public ActivateUserResponse validateOtpAndActivateUser(ActivateUserAccountDto request) {
        ActivateUserResponse activateUserResponse = userService.activateUser(request);
        ProjectOwner projectOwner = projectOwnerRepository.findProjectOwnerByUserId(activateUserResponse.getUserId());
        projectOwner.setIsActive(true);
        projectOwner.setUpdatedDate(LocalDateTime.now());
        projectOwnerRepository.save(projectOwner);
        return activateUserResponse;
    }

    public UserActivationResponse accountActivation(PasswordActivationRequest request) {
        ProjectOwner projectOwner = projectOwnerRepository.findByUserId(request.getId());
        User user = userRepository.findById(request.getId()).get();
        if(Objects.nonNull(user.getLastLogin())) {
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST,
                    "User account is already active");
        }
        if(Objects.isNull(projectOwner)) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                    "Project owner does not exist for this user Id");
        }
        if(projectOwner.getIsActive()){
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST,
                    "Project Owner account is already active");
        }

        projectOwner.setIsActive(true);
        projectOwnerRepository.save(projectOwner);
        return userService.userPasswordActivation(request);
    }

    public HashMap<String, Integer> getProjectSummary(Long projectOwnerId) {
        projectOwnerRepository.findById(projectOwnerId)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Project Owner Id does not exist!"));

        int projectsCreated = projectRepository.findByProjectOwnerId(projectOwnerId).size();
        int totalSurveys = submissionService.getSurveysForProject(projectRepository.findByProjectOwnerId(projectOwnerId), null);
        int projectsInProgress = projectRepository.findByProjectOwnerIdAndStatus(projectOwnerId, Status.ONGOING).size();
        int submittedSurveys = submissionService.getSurveysForProject(projectRepository.findByProjectOwnerId(projectOwnerId), SubmissionStatus.ACCEPTED);
        int completedProjects = projectRepository.findByProjectOwnerIdAndStatus(projectOwnerId, Status.COMPLETED).size();
        int unSubmittedSurveys = totalSurveys - submittedSurveys;

        return new HashMap<String, Integer>() {{
            put("projectsCreated", projectsCreated);
            put("totalSurveys", totalSurveys);
            put("projectsInProgress", projectsInProgress);
            put("submittedSurveys", submittedSurveys);
            put("completedProjects", completedProjects);
            put("unSubmittedSurveys", unSubmittedSurveys);
        }};
    }



    public HashMap<String, Integer> getProjectSummary(Long projectOwnerId, String startDate, String endDate) {
        projectOwnerRepository.findById(projectOwnerId)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Project Owner Id does not exist!"));
        LocalDateTime start = DateFormatter.convertToLocalDate(startDate);
        LocalDateTime end = Objects.nonNull(endDate) ? DateFormatter.convertToLocalDate(endDate) : LocalDateTime.now();
        DateFormatter.checkStartAndEndDate(start, end);

        int projectsCreated = projectRepository.findByProjectOwnerIdAndCreatedDateBetween(projectOwnerId, start, end).size();
        int totalSurveys = submissionService.getSurveysForProject(projectRepository.findByProjectOwnerIdAndCreatedDateBetween(projectOwnerId, start, end), null);
        int projectsInProgress = projectRepository.findByProjectOwnerIdAndStatusAndCreatedDateBetween(projectOwnerId, Status.ONGOING, start, end).size();
        int submittedSurveys = submissionService.getSurveysForProject(projectRepository.findByProjectOwnerIdAndCreatedDateBetween(projectOwnerId, start, end), SubmissionStatus.ACCEPTED);
        int completedProjects = projectRepository.findByProjectOwnerIdAndStatusAndCreatedDateBetween(projectOwnerId, Status.COMPLETED, start, end).size();
        int unSubmittedSurveys = totalSurveys - submittedSurveys;

        return new HashMap<String, Integer>() {{
            put("projectsCreated", projectsCreated);
            put("totalSurveys", totalSurveys);
            put("projectsInProgress", projectsInProgress);
            put("submittedSurveys", submittedSurveys);
            put("completedProjects", completedProjects);
            put("unSubmittedSurveys", unSubmittedSurveys);
        }};
    }

    private ProjectOwner setTransientFields(ProjectOwner projectOwner){
        if(projectOwner.getLgaId() != null) {
            Optional<LGA> lga = lgaRepository.findById(projectOwner.getLgaId());
            if(lga.isPresent())
                projectOwner.setLga(lga.get().getName());
        }
        if(projectOwner.getSectorId() != null){
            Optional<Sector> sector = sectorRepository.findById(projectOwner.getSectorId());
            if(sector.isPresent())
                projectOwner.setSector(sector.get().getName());
        }
        if(projectOwner.getId() != null) {
            List<Project> projects = projectRepository.findByProjectOwnerId(projectOwner.getId());
            if(projects.size() > 0)
            projectOwner.setProjectCount(projects.size());
        }
        if(projectOwner.getUserId() != null) {
            Optional<User> user = userRepository.findById(projectOwner.getUserId());
            if(user.isPresent())
                projectOwner.setUserIsActive(user.get().getIsActive());
        }
        return projectOwner;
    }

    public ProjectOwnerEnumeratorKyc getProjectOwnerKYC(Long id) {
//        userRepository.findById(userId)
//                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
//                        "Requested user Id does not exist!"));
        ProjectOwner projectOwner = projectOwnerRepository.findProjectOwnerById(id);
        if(Objects.isNull(projectOwner)) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                    "Requested project owner object does not exist");
        }
        ProjectOwnerEnumeratorKyc projectOwnerEnumeratorKyc = new ProjectOwnerEnumeratorKyc();
        projectOwnerEnumeratorKyc.setAddress(projectOwner.getAddress());
        projectOwnerEnumeratorKyc.setFirstName(projectOwner.getFirstname());
        projectOwnerEnumeratorKyc.setLastName(projectOwner.getLastname());
        projectOwnerEnumeratorKyc.setVerificationStatus(projectOwner.getCorporateName());
        projectOwnerEnumeratorKyc.setEmail(projectOwner.getEmail());
//        enumeratorResponse.setCardImage(enumerator.);
//        enumeratorResponse.setIdCardNumber(enumerator);
        projectOwnerEnumeratorKyc.setAccountManager(projectOwner.getAccountManager());
        projectOwnerEnumeratorKyc.setCAC(projectOwner.getCAC());
//        projectOwnerEnumeratorKyc.setO(projectOwner.getOrganisationType());
        return mapper.map(projectOwnerEnumeratorKyc, ProjectOwnerEnumeratorKyc.class);
    }

    private void createProjectOwnerWallet(Long userId) {
        CreateWalletDto createWalletDto = new CreateWalletDto();
        createWalletDto.setUserId(userId);
        dataWalletService.createWalletOnSignUp(createWalletDto);
    }
}
