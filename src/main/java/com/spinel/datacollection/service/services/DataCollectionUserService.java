package com.spinel.datacollection.service.services;

import com.google.gson.Gson;
import com.spinel.datacollection.core.dto.request.DataCollectionUserRequestDto;
import com.spinel.datacollection.core.dto.response.DataCollectionUserResponseDto;
import com.spinel.datacollection.core.enums.UserCategory;
import com.spinel.datacollection.service.helper.Validations;
import com.spinel.framework.exceptions.ConflictException;
import com.spinel.framework.models.PreviousPasswords;
import com.spinel.framework.models.User;
import com.spinel.framework.models.UserRole;
import com.spinel.framework.notification.requestDto.NotificationRequestDto;
import com.spinel.framework.notification.requestDto.RecipientRequest;
import com.spinel.framework.repositories.PreviousPasswordRepository;
import com.spinel.framework.repositories.UserRepository;
import com.spinel.framework.repositories.UserRoleRepository;
import com.spinel.framework.service.AuditTrailService;
import com.spinel.framework.service.NotificationService;
import com.spinel.framework.service.TokenService;
import com.spinel.framework.service.WhatsAppService;
import com.spinel.framework.utils.AuditTrailFlag;
import com.spinel.framework.utils.CustomResponseCode;
import com.spinel.framework.utils.Utility;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DataCollectionUserService {

    private final ModelMapper mapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private PreviousPasswordRepository previousPasswordRepository;

    @Autowired
    private Validations validations;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private WhatsAppService whatsAppService;

    public DataCollectionUserService(ModelMapper mapper, Validations validations) {
        this.mapper = mapper;
        this.validations = validations;
    }

    public DataCollectionUserResponseDto createDataUser(DataCollectionUserRequestDto request, HttpServletRequest request1) {
        validations.validateDataUser(request);
        User user = mapper.map(request,User.class);

        User userExist = userRepository.findByEmailOrPhone(request.getEmail(),request.getPhone());
        if(userExist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " User already exist");
        }
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();

//        SupplierUser supplierUser = supplierUserRepository.findByUserId(userCurrent.getId());

        String password = Utility.getSaltString();
        user.setPassword(passwordEncoder.encode(password));
        user.setUsername(request.getEmail());
        user.setCreatedBy(userCurrent.getId());
        if (request.getUserCategory().equals(UserCategory.ENUMERATOR)){
            user.setUserCategory(UserCategory.ENUMERATOR.toString());
        } else if (request.getUserCategory().equals(UserCategory.PROJECT_OWNER)){
            user.setUserCategory(UserCategory.PROJECT_OWNER.toString());
        } else{
            user.setUserCategory(UserCategory.ADMIN.toString());
        }
        user.setResetToken(Utility.registrationCode("HHmmss"));
        user.setResetTokenExpirationDate(Utility.tokenExpiration());
//        user.setUserCategory(String.valueOf(request.getUserCategory()));
//        user.setClientId(supplierUser.getSupplierId());
        user.setIsActive(false);
        user.setLoginAttempts(0);
        log.info("User to save :::::::::::::::::::::: " + user);
        user = userRepository.save(user);
        log.debug("Create new Data Co user - {}"+ new Gson().toJson(user));

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
        NotificationRequestDto notificationRequestDto = new NotificationRequestDto();
        User emailRecipient = (User)this.userRepository.getOne(user.getId());
        notificationRequestDto.setMessage("Activation Otp  " + user.getResetToken());
        List<RecipientRequest> recipient = new ArrayList();
        recipient.add(RecipientRequest.builder().email(emailRecipient.getEmail()).build());
//        notificationRequestDto.setRecipient(String.valueOf(recipient));
        notificationRequestDto.setRecipient(emailRecipient.getEmail());
        this.notificationService.emailNotificationRequest(notificationRequestDto);
//        SmsRequest smsRequest = SmsRequest.builder().message("Activation Otp  " + user.getResetToken()).phoneNumber(emailRecipient.getPhone()).build();
//        this.notificationService.smsNotificationRequest(smsRequest);
//        WhatsAppRequest whatsAppRequest = WhatsAppRequest.builder().message("Activation Otp  " + user.getResetToken()).phoneNumber(emailRecipient.getPhone()).build();
//        this.whatsAppService.whatsAppNotification(whatsAppRequest);

//        SupplierUser supplier = new SupplierUser();
//        supplier.setSupplierId(supplierUser.getSupplierId());
//        supplier.setUserId(user.getId());
//        supplier.setCreatedBy(userCurrent.getId());
//        supplier.setIsActive(true);
//        supplierUserRepository.save(supplier);
//        log.debug("save to supplier user table - {}"+ new Gson().toJson(supplier));

        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Create new Data CO by :" + userCurrent.getUsername(),
                        AuditTrailFlag.CREATE,
                        " Create new Data Co for:" + user.getUsername() ,1, Utility.getClientIp(request1));
        return mapper.map(user, DataCollectionUserResponseDto.class);
    }
}
