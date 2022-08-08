package com.spinel.datacollection.service.services;

import com.fasterxml.jackson.core.JsonProcessingException;

import com.spinel.datacollection.core.enums.UserCategory;
import com.spinel.datacollection.core.models.Enumerator;
import com.spinel.datacollection.core.models.ProjectOwner;
import com.spinel.datacollection.service.repositories.EnumeratorRepository;
import com.spinel.datacollection.service.repositories.ProjectOwnerRepository;


import com.spinel.framework.dto.requestDto.LoginRequest;
import com.spinel.framework.dto.responseDto.AccessTokenWithUserDetails;
import com.spinel.framework.dto.responseDto.PartnersCategoryReturn;
import com.spinel.framework.exceptions.LockedException;
import com.spinel.framework.exceptions.UnauthorizedException;
import com.spinel.framework.models.User;
import com.spinel.framework.repositories.PermissionRepository;
import com.spinel.framework.security.AuthenticationWithToken;
import com.spinel.framework.service.*;
import com.spinel.framework.utils.AuditTrailFlag;
import com.spinel.framework.utils.CustomResponseCode;
import com.spinel.framework.utils.Utility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("ALL")
@Slf4j
@Service
public class AuthenticationService {

    @Value("${login.attempts}")
    private int loginAttempts;

    private final UserService userService;
    private final AuditTrailService auditTrailService;
    private final PermissionService permissionService;
    private final EnumeratorRepository enumeratorRepository;
    private final ProjectOwnerRepository projectOwnerRepository;
    private final TokenService tokenService;

    public AuthenticationService(UserService userService, AuditTrailService auditTrailService,
                                 PermissionRepository permissionRepository, PermissionService permissionService,
                                 UserRoleService userRoleService, EnumeratorRepository enumeratorRepository,
                                 ProjectOwnerRepository projectOwnerRepository, TokenService tokenService) {
        this.userService = userService;
        this.auditTrailService = auditTrailService;
        this.permissionService = permissionService;
        this.enumeratorRepository = enumeratorRepository;
        this.projectOwnerRepository = projectOwnerRepository;
        this.tokenService = tokenService;
    }

    public AccessTokenWithUserDetails loginUser(LoginRequest loginRequest, HttpServletRequest request) {
        log.info(":::::::::: login Request %s:::::::::::::" + loginRequest.getUsername());
        String loginStatus;
        String ipAddress = Utility.getClientIp(request);
        User user = userService.loginUser(loginRequest);
        if(Objects.isNull(user)) {
            throw new UnauthorizedException(CustomResponseCode.UNAUTHORIZED, "Invalid Login details.");
        }
        Enumerator enumerator = userIsEnumerator(user);
        ProjectOwner projectOwner = userIsProjectOwner(user);
        if(Objects.nonNull(enumerator) || Objects.nonNull(projectOwner)) {
            if (user.isLoginStatus()) {
                validateUser(user);
            } else {
                //update login failed count and failed login date
                loginStatus = "failed";
                auditTrailService
                        .logEvent(loginRequest.getUsername(), "Login by username :" + loginRequest.getUsername()
                                        + " " + loginStatus,
                                AuditTrailFlag.LOGIN, "Failed Login Request by :" + loginRequest.getUsername(),1, ipAddress);
                userService.updateFailedLogin(user.getId());
                throw new UnauthorizedException(CustomResponseCode.UNAUTHORIZED, "Invalid Login details.");
            }
        } else {
            //NO NEED TO update login failed count and failed login date SINCE IT DOES NOT EXIST
            throw new UnauthorizedException(CustomResponseCode.UNAUTHORIZED, "Login details does not exist");
        }
        return completeLogin(loginRequest, user, ipAddress);
    }

    public AccessTokenWithUserDetails loginAdminUser(@RequestBody @Valid LoginRequest loginRequest, HttpServletRequest request) throws JsonProcessingException {

        log.info(":::::::::: login Request %s:::::::::::::" + loginRequest.getUsername());
        String loginStatus;
        String ipAddress = Utility.getClientIp(request);
        User user = userService.loginUser(loginRequest);
        if(!userIsAdmin(user)) {
            throw new UnauthorizedException(CustomResponseCode.UNAUTHORIZED, "Invalid Login details.");
        }
        if (user.isLoginStatus()) {
            validateUser(user);
        } else {
            //update login failed count and failed login date
            loginStatus = "failed";
            auditTrailService
                    .logEvent(loginRequest.getUsername(), "Login by username :" + loginRequest.getUsername()
                                    + " " + loginStatus,
                            AuditTrailFlag.LOGIN, "Failed Login Request by :" + loginRequest.getUsername(),1, ipAddress);
            userService.updateFailedLogin(user.getId());
            throw new UnauthorizedException(CustomResponseCode.UNAUTHORIZED, "Invalid Login details.");
        }
        return completeLogin(loginRequest, user, ipAddress);
    }

    private AccessTokenWithUserDetails completeLogin(LoginRequest loginRequest, User user, String ipAddress) {
        String accessList = permissionService.getPermissionsByUserId(user.getId());
        AuthenticationWithToken authWithToken = new AuthenticationWithToken(user, null,
                AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_USER,"+accessList));
        String newToken = "Bearer" +" "+this.tokenService.generateNewToken();
        authWithToken.setToken(newToken);
        tokenService.store(newToken, authWithToken);
        SecurityContextHolder.getContext().setAuthentication(authWithToken);
        userService.updateLogin(user.getId());

        String clientId= "";
        String referralCode="";
        String isEmailVerified="";
        List<PartnersCategoryReturn> partnerCategory= null;

        AccessTokenWithUserDetails details = new AccessTokenWithUserDetails(newToken, user,
                accessList,userService.getSessionExpiry(),clientId,referralCode,isEmailVerified);
        auditTrailService
                .logEvent(loginRequest.getUsername(), "Login by username : " + loginRequest.getUsername(),
                        AuditTrailFlag.LOGIN, "Successful Login Request by : " + loginRequest.getUsername() , 1, ipAddress);
        return details;
    }

    private void validateUser(User user) {
        if (user.getPasswordChangedOn() == null) {
            throw new UnauthorizedException(CustomResponseCode.UNAUTHORIZED,
                    "Change password Required, account has not been activated");
        }
        if (!user.getIsActive()) {
            throw new UnauthorizedException(CustomResponseCode.UNAUTHORIZED,
                    "User Account Deactivated, please contact Administrator");
        }
        if (user.getLoginAttempts() >= loginAttempts || user.getLockedDate() != null) {
            // lock account after x failed attempts or locked date is not null
            userService.lockLogin(user.getId());
            throw new LockedException(CustomResponseCode.LOCKED_EXCEPTION, "This account has been locked, Kindly contact support");
        }
    }

    private Enumerator userIsEnumerator(User user) {
        Enumerator enumerator = enumeratorRepository.findByUserId(user.getId());
        if (Objects.nonNull(enumerator) && !enumerator.getIsActive()) {
            throw new UnauthorizedException(CustomResponseCode.UNAUTHORIZED,
                    "Enumerator Account Deactivated, please contact Administrator");
        }
        return enumerator;
    }

    private ProjectOwner userIsProjectOwner(User user) {
        ProjectOwner projectOwner = projectOwnerRepository.findByUserId(user.getId());
        if (Objects.nonNull(projectOwner) && !projectOwner.getIsActive()) {
            throw new UnauthorizedException(CustomResponseCode.UNAUTHORIZED,
                    "Project Owner Account Deactivated, please contact Administrator");
        }
        return projectOwner;
    }

    private boolean userIsAdmin(User user) {
        if(Objects.isNull(user)) {
            throw new UnauthorizedException(CustomResponseCode.UNAUTHORIZED, "Login details does not exist");
        }
        return user.getUserCategory().equals(UserCategory.ADMIN.toString());
    }

}
