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
import com.sabi.datacollection.core.models.Enumerator;
import com.sabi.datacollection.core.models.LGA;
import com.sabi.datacollection.core.models.OrganisationType;
import com.sabi.datacollection.core.models.State;
import com.sabi.datacollection.service.helper.DataConstants;
import com.sabi.datacollection.service.helper.Validations;
import com.sabi.datacollection.service.repositories.EnumeratorRepository;
import com.sabi.datacollection.service.repositories.LGARepository;
import com.sabi.datacollection.service.repositories.OrganisationTypeRepository;
import com.sabi.datacollection.service.repositories.StateRepository;
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
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


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


    public EnumeratorService(EnumeratorRepository repository, UserRepository userRepository,
                             PreviousPasswordRepository previousPasswordRepository, ModelMapper mapper,
                             ObjectMapper objectMapper, Validations validations, NotificationService notificationService,
                             LGARepository lgaRepository, AuditTrailService auditTrailService,
                             StateRepository stateRepository, UserRoleRepository userRoleRepository) {
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
        user.setUserCategory(DataConstants.ENUMERATOR_USER);
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
        saveEnumerator.setUserId(user.getId());
        saveEnumerator.setIsActive(false);
        saveEnumerator.setCreatedBy(user.getId());
        saveEnumerator.setCorp(request.getIsCorp());
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



    public CompleteSignUpResponse completeSignUp(CompleteSignupRequest request) {
        validations.validateEnumeratorProperties(request);
        Enumerator enumerator = repository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Enumerator Id does not exist!"));
        mapper.map(request, enumerator);

        enumerator.setUpdatedBy(enumerator.getUserId());
        enumerator.setIsActive(true);
        repository.save(enumerator);
        log.debug("complete signup  - {}"+ new Gson().toJson(enumerator));

        User user = userRepository.getOne(enumerator.getUserId());
        user.setIsActive(true);
        user.setUpdatedBy(enumerator.getUserId());
        user.setPasswordChangedOn(LocalDateTime.now());
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
        LGA lga = lgaRepository.findLGAById(enumeratorProperties.getLgaId());

        State state = stateRepository.getOne(lga.getStateId());

        OrganisationType organisationType = organisationTypeRepository.findOrganisationTypeById(enumeratorProperties.getOrganisationTypeId());


        enumeratorProperties.setOrganisationType(organisationType.getName());
        enumeratorProperties.setLga(lga.getName());
        enumeratorProperties.setState(state.getName());

        return mapper.map(enumeratorProperties,EnumeratorResponseDto.class);
    }


    public Page<Enumerator> findAll(String name, PageRequest pageRequest ){
        Page<Enumerator> enumeratorProperties = repository.findEnumeratorsProperties(name,pageRequest);
        if(enumeratorProperties == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        enumeratorProperties.getContent().forEach(enumerator ->{
            LGA lga = lgaRepository.findLGAById(enumerator.getLgaId());
            OrganisationType organisationType = organisationTypeRepository.findOrganisationTypeById(enumerator.getOrganisationTypeId());


            enumerator.setOrganisationType(organisationType.getName());
            enumerator.setLga(lga.getName());
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
        List<Enumerator> enumeratorProperties = repository.findByIsActive(isActive);
        for (Enumerator part : enumeratorProperties
                ) {
            LGA lga = lgaRepository.findLGAById(part.getLgaId());
            OrganisationType organisationType = organisationTypeRepository.findOrganisationTypeById(part.getOrganisationTypeId());


            if (organisationType == null){
                throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, "Organisation type is null");
            }
            part.setOrganisationType(organisationType.getName());

            if (lga == null){
                throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, "LGA type is null");
            }
            part.setLga(lga.getName());

        }
        return enumeratorProperties;

    }
}
