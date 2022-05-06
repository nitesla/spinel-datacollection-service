package com.sabi.datacollection.service.services;


import com.google.gson.Gson;
import com.sabi.datacollection.core.enums.AuditTrailFlag;
import com.sabi.datacollection.core.models.DataRole;
import com.sabi.datacollection.service.helper.Validations;
import com.sabi.datacollection.service.repositories.DataRoleRepository;
import com.sabi.datacollection.service.repositories.DataUserRepository;
import com.sabi.framework.dto.requestDto.*;
import com.sabi.framework.dto.responseDto.ActivateUserResponse;
import com.sabi.framework.dto.responseDto.UserResponse;
import com.sabi.framework.exceptions.BadRequestException;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.helpers.Encryptions;
import com.sabi.framework.models.PreviousPasswords;
import com.sabi.framework.models.Role;
import com.sabi.framework.models.User;
import com.sabi.framework.models.UserRole;
import com.sabi.framework.notification.requestDto.*;
import com.sabi.framework.repositories.PreviousPasswordRepository;
import com.sabi.framework.repositories.UserRoleRepository;
import com.sabi.framework.service.NotificationService;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.service.WhatsAppService;
import com.sabi.framework.utils.Constants;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.framework.utils.Utility;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@SuppressWarnings("ALL")
@Slf4j
@Service
public class DataUserService {

    @Value("${token.time.to.leave}")
    long tokenTimeToLeave;


    private final PasswordEncoder passwordEncoder;
    private final PreviousPasswordRepository previousPasswordRepository;
    private final DataUserRepository userRepository;
    private final NotificationService notificationService;
    private final ModelMapper mapper;
    private final Validations validations;
    private final DataAuditTrailService auditTrailService;
    private final WhatsAppService whatsAppService;
    private final DataRoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    public DataUserService(PasswordEncoder passwordEncoder, PreviousPasswordRepository previousPasswordRepository, DataUserRepository userRepository, NotificationService notificationService, ModelMapper mapper, Validations validations, DataAuditTrailService auditTrailService, WhatsAppService whatsAppService, DataRoleRepository roleRepository, UserRoleRepository userRoleRepository) {
        this.passwordEncoder = passwordEncoder;
        this.previousPasswordRepository = previousPasswordRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.mapper = mapper;
        this.validations = validations;
        this.auditTrailService = auditTrailService;
        this.whatsAppService = whatsAppService;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
    }

    /** <summary>
     * User creation
     * </summary>
     * <remarks>this method is responsible for creation of new user</remarks>
     */

    public UserResponse createUser(UserDto request, HttpServletRequest request1) {
        validations.validateUser(request);
        User userExist = userRepository.findByEmailOrPhone(request.getEmail(),request.getPhone());
        if(userExist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " User already exist");
        }
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        User user = mapper.map(request,User.class);
//        String password = request.getPassword();
        String password = Utility.getSaltString();
        user.setPassword(passwordEncoder.encode(password));
        user.setUsername(request.getEmail());
        user.setCreatedBy(userCurrent.getId());
        user.setUserCategory(Constants.ADMIN_USER);
        user.setIsActive(false);
        user.setLoginAttempts(0);
        user.setResetToken(Utility.registrationCode("HHmmss"));
        user.setResetTokenExpirationDate(Utility.tokenExpiration());
        user = userRepository.save(user);
        log.debug("Create new user - {}"+ new Gson().toJson(user));

        UserRole userRole = UserRole.builder()
                .userId(user.getId())
                .roleId(user.getRoleId())
//                .createdDate(LocalDateTime.now())
                .build();
        userRoleRepository.save(userRole);

        PreviousPasswords previousPasswords = PreviousPasswords.builder()
                .userId(user.getId())
                .password(user.getPassword())
                .createdDate(LocalDateTime.now())
                .build();
        previousPasswordRepository.save(previousPasswords);

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
                .logEvent(userCurrent.getUsername(),
                        "Create new user by :" + userCurrent.getUsername(),
                        AuditTrailFlag.CREATE,
                        " Create new user for:" + user.getFirstName() + " " + user.getUsername(),1, Utility.getClientIp(request1));
        return mapper.map(user, UserResponse.class);
    }



    /** <summary>
     * User update
     * </summary>
     * <remarks>this method is responsible for updating already existing user</remarks>
     */

    public UserResponse updateUser(UserDto request,HttpServletRequest request1) {
        validations.updateUser(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        User user = userRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested user id does not exist!"));
        mapper.map(request, user);
        user.setUpdatedBy(userCurrent.getId());
        userRepository.save(user);
        log.debug("user record updated - {}"+ new Gson().toJson(user));

//        UserRole userRole = UserRole.builder()
//                .userId(user.getId())
//                .roleId(user.getRoleId())
////                .createdDate(LocalDateTime.now())
//                .build();
//        userRoleRepository.save(userRole);

        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Update user by username:" + userCurrent.getUsername(),
                        AuditTrailFlag.UPDATE,
                        " Update user Request for:" + user.getId() + " "+ user.getUsername(),1, Utility.getClientIp(request1));
        return mapper.map(user, UserResponse.class);
    }



    /** <summary>
     * Find user
     * </summary>
     * <remarks>this method is responsible for getting a single record</remarks>
     */
    public UserResponse findUser(Long id){
        User user  = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested user id does not exist!"));

        DataRole role = roleRepository.getOne(user.getRoleId());
        UserResponse userResponse = UserResponse.builder()
                .createdBy(user.getCreatedBy())
                .createdDate(user.getCreatedDate())
                .email(user.getEmail())
                .failedLoginDate(user.getFailedLoginDate())
                .firstName(user.getFirstName())
                .id(user.getId())
                .isActive(user.getIsActive())
                .lastLogin(user.getLastLogin())
                .lastName(user.getLastName())
                .middleName(user.getMiddleName())
                .phone(user.getPhone())
                .updatedBy(user.getUpdatedBy())
                .updatedDate(user.getUpdatedDate())
                .roleId(user.getRoleId())
                .roleName(role.getName())
                .photo(user.getPhoto())
                .build();


        return userResponse;
    }



    /** <summary>
     * Find all users
     * </summary>
     * <remarks>this method is responsible for getting all records in pagination</remarks>
     */
    public Page<User> findAll(String firstName, String lastName, String phone, Boolean isActive, String email, PageRequest pageRequest ){
        Page<User> users = userRepository.findUsers(firstName,lastName,phone,isActive,email,pageRequest);
        if(users == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return users;

    }







    /** <summary>
     * Enable disenable
     * </summary>
     * <remarks>this method is responsible for enabling and dis enabling a user</remarks>
     */
    public void enableDisEnableUser (EnableDisEnableDto request, HttpServletRequest request1){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        User user  = userRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested user id does not exist!"));
        user.setIsActive(request.isActive());
        user.setUpdatedBy(userCurrent.getId());

        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Disable/Enable user by :" + userCurrent.getUsername() ,
                        AuditTrailFlag.UPDATE,
                        " Disable/Enable user Request for:" +  user.getId()
                                + " " +  user.getUsername(),1, Utility.getClientIp(request1));
        userRepository.save(user);

    }



    /** <summary>
     * Change password
     * </summary>
     * <remarks>this method is responsible for changing password</remarks>
     */

    public void changeUserPassword(ChangePasswordDto request) {
        validations.changePassword(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        User user = userRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested user id does not exist!"));
        mapper.map(request, user);
        if(getPrevPasswords(user.getId(),request.getPassword())){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Password already used");
        }
        if (!getPrevPasswords(user.getId(), request.getPreviousPassword())) {
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid previous password");
        }
        String password = request.getPassword();
        user.setPassword(passwordEncoder.encode(password));
        user.setIsActive(true);
        user.setLockedDate(null);
        user.setUpdatedBy(userCurrent.getId());
        user = userRepository.save(user);

        PreviousPasswords previousPasswords = PreviousPasswords.builder()
                .userId(user.getId())
                .password(user.getPassword())
                .createdDate(LocalDateTime.now())
                .build();
        previousPasswordRepository.save(previousPasswords);

    }


    /** <summary>
     * Previous password
     * </summary>
     * <remarks>this method is responsible for fetching the last 4 passwords</remarks>
     */

    public Boolean getPrevPasswords(Long userId,String password){
        List<PreviousPasswords> prev = previousPasswordRepository.previousPasswords(userId);
        for (PreviousPasswords pass : prev
        ) {
            if (passwordEncoder.matches(password, pass.getPassword())) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }





    /** <summary>
     * Unlock account
     * </summary>
     * <remarks>this method is responsible for unlocking a user account</remarks>
     */
    public void unlockAccounts (UnlockAccountRequestDto request) {
        User user = userRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested user id does not exist!"));
        mapper.map(request, user);
        user.setLockedDate(null);
        user.setLoginAttempts(0);
        userRepository.save(user);

    }



    /** <summary>
     * Lock account
     * </summary>
     * <remarks>this method is responsible for lock a user account</remarks>
     */
    public void lockLogin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested user id does not exist!"));
        user.setLockedDate(new Date());
        userRepository.save(user);
    }


    /** <summary>
     * Forget password
     * </summary>
     * <remarks>this method is responsible for a user that forgets his password</remarks>
     */

    public void forgetPassword (ForgetPasswordDto request) {

        if(request.getEmail() != null) {

            User user = userRepository.findByEmail(request.getEmail());
            if (user == null) {
                throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, "Invalid email");
            }
            if (user.getIsActive() == false) {
                throw new BadRequestException(CustomResponseCode.FAILED, "User account has been disabled");
            }
            user.setResetToken(Utility.registrationCode("HHmmss"));
            user.setResetTokenExpirationDate(Utility.tokenExpiration());
            userRepository.save(user);

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

        }else if(request.getPhone()!= null) {

            User userPhone = userRepository.findByPhone(request.getPhone());
            if (userPhone == null) {
                throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, "Invalid phone number");
            }
            if (userPhone.getIsActive() == false) {
                throw new BadRequestException(CustomResponseCode.FAILED, "User account has been disabled");
            }
            userPhone.setResetToken(Utility.registrationCode("HHmmss"));
            userPhone.setResetTokenExpirationDate(Utility.tokenExpiration());
            userRepository.save(userPhone);


            NotificationRequestDto notificationRequestDto = new NotificationRequestDto();
            User emailRecipient = userRepository.getOne(userPhone.getId());
            notificationRequestDto.setMessage("Activation Otp " + " " + userPhone.getResetToken());
            List<RecipientRequest> recipient = new ArrayList<>();
            recipient.add(RecipientRequest.builder()
                    .email(emailRecipient.getEmail())
                    .build());
            notificationRequestDto.setRecipient(recipient);
            notificationService.emailNotificationRequest(notificationRequestDto);

            SmsRequest smsRequest = SmsRequest.builder()
                    .message("Activation Otp " + " " + userPhone.getResetToken())
                    .phoneNumber(emailRecipient.getPhone())
                    .build();
            notificationService.smsNotificationRequest(smsRequest);

            WhatsAppRequest whatsAppRequest = WhatsAppRequest.builder()
                    .message("Activation Otp " + " " + userPhone.getResetToken())
                    .phoneNumber(emailRecipient.getPhone())
                    .build();
            whatsAppService.whatsAppNotification(whatsAppRequest);

            VoiceOtpRequest voiceOtpRequest = VoiceOtpRequest.builder()
                    .message("Activation Otp is " + " " + userPhone.getResetToken())
                    .phoneNumber(emailRecipient.getPhone())
                    .build();
            notificationService.voiceOtp(voiceOtpRequest);
        }

    }

    /** <summary>
     * Activate user
     * </summary>
     * <remarks>this method is responsible for activating users</remarks>
     */

    public ActivateUserResponse activateUser (ActivateUserAccountDto request) {
        User user = userRepository.findByResetToken(request.getResetToken());
        if(user == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, "Invalid OTP supplied");
        }
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar calobj = Calendar.getInstance();
        String currentDate = df.format(calobj.getTime());
        String regDate = user.getResetTokenExpirationDate();
        String result = String.valueOf(currentDate.compareTo(regDate));
        if(result.equals("1")){
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, " OTP invalid/expired");
        }

        request.setUpdatedBy(0l);
        request.setIsActive(true);
        request.setPasswordChangedOn(LocalDateTime.now());
        userOTPValidation(user,request);

        ActivateUserResponse response = ActivateUserResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .build();

        return response;

    }



    public User userOTPValidation(User user, ActivateUserAccountDto activateUserAccountDto) {
        user.setUpdatedBy(activateUserAccountDto.getUpdatedBy());
        user.setIsActive(activateUserAccountDto.getIsActive());
        user.setPasswordChangedOn(activateUserAccountDto.getPasswordChangedOn());
        return userRepository.saveAndFlush(user);
    }


    /** <summary>
     * Set Transaction pin
     * </summary>
     * <remarks>this method is responsible for setting new transaction pin</remarks>
     */
    public void setPin (ChangeTransactionPin request) {
        validations.changeTransactionPin(request);
        User user = userRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested user id does not exist!"));

        String auth = Encryptions.generateSha256(request.getOldTransactionPin());
        if(!auth.matches(user.getTransactionPin())){
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid old pin");
        }
        String pin = Encryptions.generateSha256(request.getTransactionPin());
        user.setTransactionPin(pin);
        userRepository.save(user);

    }


    /** <summary>
     * Change Transaction pin OTP
     * </summary>
     * <remarks>this method is responsible for changing transaction pin OTP</remarks>
     */
    public void changePinOTP (CreateTransactionPinDto request) {
        User user = userRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested user id does not exist!"));

        if (!matchPasswords(user.getId(), request.getPassword())) {
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Invalid password");
        }
        user.setResetToken(Utility.registrationCode("HHmmss"));
        user.setResetTokenExpirationDate(Utility.tokenExpiration());
        user = userRepository.save(user);


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


    }


    public Boolean matchPasswords(Long id,String password){
        User prev = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested user id does not exist!"));
        if (passwordEncoder.matches(password,prev.getPassword())) {
            return Boolean.TRUE;
        }

        return Boolean.FALSE;
    }



    /** <summary>
     * Change Transaction pin
     * </summary>
     * <remarks>this method is responsible for changing transaction pin </remarks>
     */

    public  void changePin (CreateTransactionPinDto request) {
        validations.validateTransactionPin(request);
        User userExist = userRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested user id does not exist!"));
        User user = userRepository.findByResetToken(request.getResetToken());
        if(user == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, "Invalid OTP supplied");
        }
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar calobj = Calendar.getInstance();
        String currentDate = df.format(calobj.getTime());
        String regDate = user.getResetTokenExpirationDate();
        String result = String.valueOf(currentDate.compareTo(regDate));
        if(result.equals("1")){
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, " OTP invalid/expired");
        }
        String pin = Encryptions.generateSha256(request.getTransactionPin());
        userExist.setTransactionPin(pin);
        userExist.setTransactionPinStatus(CustomResponseCode.TRANSACTION_PIN_STATUS);
        userRepository.save(userExist);

    }


    public User loginUser(LoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername());
        if (null == user) {
            return null;
        } else {

            if (passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                user.setLoginStatus(true);
            } else {
                user.setLoginStatus(false);
            }
            return user;
        }

    }


    public List<User> getAll(Boolean isActive){
        List<User> user = userRepository.findByIsActive(isActive);
        return user;

    }



    public void updateFailedLogin(Long id){
        User userExist = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " user id does not exist!"));
        if(userExist != null){
            userExist.setFailedLoginDate(LocalDateTime.now());

            int count = increment(userExist.getLoginAttempts());
            userExist.setLoginAttempts(count);
            userRepository.save(userExist);

        }
    }




    public static int increment(int number){
        // Declaring the number
        // Converting the number to String
        String string_num = Integer.toString(number);

        // Finding the length of the number
        int len = string_num.length();

        // Declaring the empty string
        String add = "";

        // Generating the addition string
        for (int i = 0; i < len; i++) {
            add = add.concat("1");
        }

        // COnverting it to Integer
        int str_num = Integer.parseInt(add);

        // Adding them and displaying the result
        System.out.println(number + str_num);

        return number + str_num;
    }






    public void updateLogin(Long id){
        User userExist = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " user id does not exist!"));
        if(userExist != null){
            userExist.setLockedDate(null);
            userExist.setLoginAttempts(0);
            userExist.setLastLogin(LocalDateTime.now());
            userRepository.save(userExist);

        }
    }





    public long getSessionExpiry() {
        //TODO Token expiry in seconds: 900 = 15mins
        return tokenTimeToLeave / 60;
    }


}
