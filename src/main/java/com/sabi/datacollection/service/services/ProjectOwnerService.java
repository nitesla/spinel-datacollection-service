package com.sabi.datacollection.service.services;

import com.sabi.datacollection.core.dto.request.CompleteSignupRequest;
import com.sabi.datacollection.core.dto.request.EnableDisableDto;
import com.sabi.datacollection.core.dto.request.ProjectOwnerDto;
import com.sabi.datacollection.core.dto.request.ProjectOwnerSignUpDto;
import com.sabi.datacollection.core.dto.response.*;
import com.sabi.datacollection.core.enums.UserCategory;
import com.sabi.datacollection.core.models.*;
import com.sabi.datacollection.service.helper.Validations;
import com.sabi.datacollection.service.repositories.*;
import com.sabi.framework.dto.requestDto.ChangePasswordDto;
import com.sabi.framework.dto.requestDto.ForgetPasswordDto;
import com.sabi.framework.exceptions.BadRequestException;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.PreviousPasswords;
import com.sabi.framework.models.User;
import com.sabi.framework.models.UserRole;
import com.sabi.framework.notification.requestDto.*;
import com.sabi.framework.repositories.PreviousPasswordRepository;
import com.sabi.framework.repositories.UserRepository;
import com.sabi.framework.repositories.UserRoleRepository;
import com.sabi.framework.service.*;
import com.sabi.framework.utils.AuditTrailFlag;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.framework.utils.Utility;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("ALL")
@Slf4j
@Service
public class ProjectOwnerService {


    @Autowired
    private NotificationService notificationService;

    @Autowired
    private WhatsAppService whatsAppService;


    private final ProjectOwnerUserRepository projectOwnerUserRepository;
    private final OrganisationTypeRepository organisationTypeRepository;
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

    public ProjectOwnerService(ProjectOwnerUserRepository projectOwnerUserRepository, OrganisationTypeRepository organisationTypeRepository, LGARepository lgaRepository,
                               AuditTrailService auditTrailService, PasswordEncoder passwordEncoder, UserRepository userRepository, PreviousPasswordRepository previousPasswordRepository,
                               ProjectOwnerRepository projectOwnerRepository, ModelMapper mapper, Validations validations, UserRoleRepository userRoleRepository, ProjectRepository projectRepository, UserService userService) {
        this.projectOwnerUserRepository = projectOwnerUserRepository;
        this.organisationTypeRepository = organisationTypeRepository;
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
                        .projectOwnerId(projectOwnerExists.getId())
                        .corporateName(projectOwnerExists.getCorporateName())
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
        if(request.getIsCorp() == true) {
            saveProjectOwner.setCorporateName(request.getCorporateName());
        }

        ProjectOwner projectOwner = projectOwnerRepository.save(saveProjectOwner);
        log.info("Created new Project Owner - {}", saveProjectOwner);

        ProjectOwnerUser projectOwnerUser = new ProjectOwnerUser();
        projectOwnerUser.setProjectOwnerId(projectOwner.getId());
        projectOwnerUser.setUserId(user.getId());
        projectOwnerUserRepository.save(projectOwnerUser);

        ProjectOwnerSignUpResponseDto response = ProjectOwnerSignUpResponseDto.builder()
                .id(user.getId())
                .projectOwnerId(projectOwner.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .corporateName(projectOwner.getCorporateName())
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
        VoiceOtpRequest voiceOtpRequest = VoiceOtpRequest.builder()
                .message("Activation Otp is " + " " + user.getResetToken())
                .phoneNumber(emailRecipient.getPhone())
                .build();
        notificationService.voiceOtp(voiceOtpRequest);
        auditTrailService
                .logEvent(response.getUsername(),
                        "SignUp Project Owner :" + response.getUsername(),
                        AuditTrailFlag.SIGNUP,
                        " SignUp Project Owner Request for:" + user.getFirstName() + " " + user.getLastName() + " " + user.getEmail()
                        , 1, Utility.getClientIp(request1));
          return response;
    }

    public CompleteSignUpResponse completeSignUp(CompleteSignupRequest request){
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

        CompleteSignUpResponse response = CompleteSignUpResponse.builder()
                .enumeratorId(projectOwner.getId())
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
                .build();
        return response;
    }

    public ProjectOwnerActivationResponse enumeratorPasswordActivation(ChangePasswordDto request) {
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

    public ProjectOwnerResponseDto createProjectOwner(ProjectOwnerDto request) {
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        ProjectOwner projectOwner = mapper.map(request, ProjectOwner.class);
        ProjectOwner projectOwnerExists = projectOwnerRepository.findProjectOwnerById(request.getId());
        if (projectOwnerExists != null) {
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "Project owner already exists");
        }
        projectOwner.setCreatedBy(userCurrent.getId());
        projectOwner.setIsActive(true);
        projectOwnerRepository.save(projectOwner);
        log.info("Created new Project owner - {}", projectOwner);
        return mapper.map(projectOwner, ProjectOwnerResponseDto.class);
    }

    public ProjectOwnerResponseDto updateProjectOwner(ProjectOwnerDto request, HttpServletRequest request1) {
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        ProjectOwner projectOwner = projectOwnerRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested project owner does not exist"));
        mapper.map(request, projectOwner);
        projectOwner.setUpdatedBy(userCurrent.getId());
        projectOwnerRepository.save(projectOwner);
        log.info("Project owner record updated - {}", projectOwner);
        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Update Project owner by username:" + userCurrent.getUsername(),
                        AuditTrailFlag.UPDATE,
                        " Update Project owner Request for:" + projectOwner.getId() ,1, Utility.getClientIp(request1));
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

    private ProjectOwner setTransientFields(ProjectOwner projectOwner){
        if(projectOwner.getLgaId() != null) {
            Optional<LGA> lga = lgaRepository.findById(projectOwner.getLgaId());
            if(lga.isPresent())
                projectOwner.setLga(lga.get().getName());
        }
        if(projectOwner.getOrganisationTypeId() != null){
            Optional<OrganisationType> organisationType = organisationTypeRepository.findById(projectOwner.getOrganisationTypeId());
            if(organisationType.isPresent())
                projectOwner.setOrganisationType(organisationType.get().getName());
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
}
