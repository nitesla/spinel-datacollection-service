package com.sabi.datacollection.service.services;


import com.google.gson.Gson;
import com.sabi.datacollection.core.dto.request.EnableDisEnableDto;
import com.sabi.datacollection.core.dto.request.DataRoleDto;
import com.sabi.datacollection.core.dto.response.DataRoleResponseDto;
import com.sabi.datacollection.core.models.DataRole;
import com.sabi.datacollection.core.models.DataRolePermission;
import com.sabi.datacollection.service.helper.Validations;
import com.sabi.datacollection.service.repositories.DataRoleRepository;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.AuditTrailService;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.AuditTrailFlag;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.framework.utils.Utility;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Slf4j
@Service
public class DataRoleService {
    private final DataRoleRepository dataRoleRepository;
    private final ModelMapper mapper;
    private final Validations validations;
    private final DataRolePermissionService dataRolePermissionService;
    private final AuditTrailService auditTrailService;

    public DataRoleService(DataRoleRepository dataRoleRepository, ModelMapper mapper, Validations validations, DataRolePermissionService dataRolePermissionService, AuditTrailService auditTrailService) {
        this.dataRoleRepository = dataRoleRepository;
        this.mapper = mapper;
        this.validations = validations;
        this.dataRolePermissionService = dataRolePermissionService;
        this.auditTrailService = auditTrailService;
    }

    public DataRoleResponseDto createRole(DataRoleDto request, HttpServletRequest request1) {
        validations.validateRole(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        DataRole role = mapper.map(request,DataRole.class);
        DataRole roleExist = dataRoleRepository.findByName(request.getName());
        if(roleExist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Role already exist");
        }
        role.setCreatedBy(userCurrent.getId());
        role.setStatus(CustomResponseCode.ACTIVE_USER);
        role = dataRoleRepository.save(role);
        log.debug("Create new role - {}"+ new Gson().toJson(role));


        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Create new role by :" + userCurrent.getUsername(),
                        AuditTrailFlag.CREATE,
                        " Create new role for:" + role.getName(),1, Utility.getClientIp(request1));
        return mapper.map(role, DataRoleResponseDto.class);
    }

    public DataRoleResponseDto updateRole(DataRoleDto request,HttpServletRequest request1) {
        validations.validateRole(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        DataRole role = dataRoleRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested role id does not exist!"));
        mapper.map(request, role);
        role.setUpdatedBy(userCurrent.getId());
        dataRoleRepository.save(role);
        log.debug("role record updated - {}"+ new Gson().toJson(role));

        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Update role by username:" + userCurrent.getUsername(),
                        AuditTrailFlag.UPDATE,
                        " Update role Request for:" + role.getId(),1, Utility.getClientIp(request1));
        return mapper.map(role, DataRoleResponseDto.class);
    }


    public DataRoleResponseDto findRole(Long id) {
        DataRole role = dataRoleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested role id does not exist!"));
        DataRoleResponseDto roleResponseDto = new DataRoleResponseDto();

        roleResponseDto.setId(role.getId());
        roleResponseDto.setName(role.getName());
        roleResponseDto.setDescription(role.getDescription());
        roleResponseDto.setCreatedBy(role.getCreatedBy());
        roleResponseDto.setCreatedDate(role.getCreatedDate());
        roleResponseDto.setUpdatedBy(role.getUpdatedBy());
        roleResponseDto.setUpdatedDate(role.getUpdatedDate());
        List<DataRolePermission> permissions= null;

        permissions = dataRolePermissionService.getPermissionsByRole(role.getId());

        roleResponseDto.setPermissions(permissions);

        return roleResponseDto;

    }



    /** <summary>
     * Find all roles
     * </summary>
     * <remarks>this method is responsible for getting all records in pagination</remarks>
     */
    public Page<DataRole> findAll(String name, Integer status, PageRequest pageRequest ){
        Page<DataRole> roles = dataRoleRepository.findRoles(name,status,pageRequest);
        if(roles == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return roles;

    }




    /** <summary>
     * Enable disable
     * </summary>
     * <remarks>this method is responsible for enabling and dis enabling a role</remarks>
     */
    public void enableDisable (EnableDisEnableDto request, HttpServletRequest request1){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        DataRole role  = dataRoleRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested role id does not exist!"));
        role.setStatus(request.getStatus());
        role.setUpdatedBy(userCurrent.getId());

        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Disable/Enable role by :" + userCurrent.getUsername() ,
                        AuditTrailFlag.UPDATE,
                        " Disable/Enable role Request for:" +  role.getId()
                                + " " +  role.getName(),1, Utility.getClientIp(request1));
        dataRoleRepository.save(role);
    }
}
