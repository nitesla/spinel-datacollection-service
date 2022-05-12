package com.sabi.datacollection.service.services;

import com.google.gson.Gson;
import com.sabi.datacollection.core.dto.request.CreateRolePermissionsDto;
import com.sabi.datacollection.core.dto.request.EnableDisEnableDto;
import com.sabi.datacollection.core.dto.response.DataRolePermissionResponseDto;
import com.sabi.datacollection.core.dto.response.DataRoleResponseDto;
import com.sabi.datacollection.core.models.DataPermission;
import com.sabi.datacollection.core.models.DataRole;
import com.sabi.datacollection.core.models.DataRolePermission;
import com.sabi.datacollection.service.helper.Validations;
import com.sabi.datacollection.service.repositories.DataPermissionRepository;
import com.sabi.datacollection.service.repositories.DataRolePermissionRepository;
import com.sabi.datacollection.service.repositories.DataRoleRepository;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.AuditTrailFlag;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.framework.utils.Utility;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class DataRolePermissionService {

    private final DataRoleRepository dataRoleRepository;
    private final DataRolePermissionRepository dataRolePermissionRepository;
    private final ModelMapper mapper;
    private final Validations validations;
    private final DataPermissionRepository dataPermissionRepository;
    private final DataAuditTrailService auditTrailService;


    public DataRolePermissionService(DataRoleRepository dataRoleRepository, DataRolePermissionRepository DataRolePermissionRepository,
                                     ModelMapper mapper,
                                     Validations validations, DataPermissionRepository dataPermissionRepository, DataAuditTrailService auditTrailService) {
        this.dataRoleRepository = dataRoleRepository;
        this.dataRolePermissionRepository = DataRolePermissionRepository;
        this.mapper = mapper;
        this.validations = validations;
        this.dataPermissionRepository = dataPermissionRepository;
        this.auditTrailService = auditTrailService;
    }

    public void assignPermission(CreateRolePermissionsDto request) {
        validations.validateRolePermission(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        DataRole role = mapper.map(request,DataRole.class);
        DataRole roleExist = dataRoleRepository.findByName(request.getRoleName());
        if(roleExist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Role already exist");
        }
        role.setCreatedBy(userCurrent.getId());
        role.setStatus(CustomResponseCode.ACTIVE_USER);
        DataRole savedRole = dataRoleRepository.save(role);
        log.debug("Create new role - {}"+ new Gson().toJson(role));

        List<DataRolePermission> rolePerm = new ArrayList<>();
        DataRolePermission rolePermission = new DataRolePermission();
        request.getPermissionIds().forEach(p -> {
            rolePermission.setPermissionId(p.getPermissionId());
            rolePermission.setRoleId(savedRole.getId());
            rolePermission.setCreatedBy(userCurrent.getId());
            log.info(" role permission details " + rolePermission);
            DataRolePermission exist = dataRolePermissionRepository.findByRoleIdAndPermissionId(request.getRoleId(),p.getPermissionId());
            if(exist != null){
                throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Permission id already assigned to the role ::::"+p.getPermissionId());
            }
            dataRolePermissionRepository.save(rolePermission);
            rolePerm.add(rolePermission);

            auditTrailService
                    .logEvent(userCurrent.getUsername(),
                            "Create new role by :" + userCurrent.getUsername(),
                            AuditTrailFlag.CREATE,
                            " Create new role for:" + role.getName(),1, Utility.getClientIp(request1));
            return mapper.map(role, DataRoleResponseDto.class);

        });
    }

    public DataRolePermissionResponseDto findRolePermission(Long id) {
        DataRolePermission rolePermission = dataRolePermissionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested RolePermission id does not exist!"));
        return mapper.map(rolePermission, DataRolePermissionResponseDto.class);
    }

    public Page<DataRolePermission> findAll(Long roleId, int status, PageRequest pageRequest) {
        Page<DataRolePermission> functions = dataRolePermissionRepository.findRolePermission(roleId, status, pageRequest);
        if (functions == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return functions;
    }

    public void enableDisEnableState(EnableDisEnableDto request) {
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        DataRolePermission creditLevel = dataRolePermissionRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested creditLevel id does not exist!"));
        creditLevel.setStatus(request.getStatus());
        creditLevel.setUpdatedBy(userCurrent.getId());
        dataRolePermissionRepository.save(creditLevel);

    }

    public List<DataRolePermission> getPermissionsByRole(Long roleId) {
        List<DataRolePermission> permissionRole = dataRolePermissionRepository.getPermissionsByRole(roleId);
        for (DataRolePermission permRole : permissionRole
        ) {
            DataPermission permission = dataPermissionRepository.getOne(permRole.getPermissionId());
            permRole.setPermission(permission.getName());
        }
        return permissionRole;
    }




    public void removePermission(Long id){
        DataRolePermission rolePermission = dataRolePermissionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested RolePermission id does not exist!"));
        dataRolePermissionRepository.delete(rolePermission);
    }


}
