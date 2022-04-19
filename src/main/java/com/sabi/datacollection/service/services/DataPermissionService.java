package com.sabi.datacollection.service.services;

import com.google.gson.Gson;
import com.sabi.datacollection.core.dto.request.DataPermissionDto;
import com.sabi.datacollection.core.dto.response.AccessListDto;
import com.sabi.datacollection.core.dto.response.DataPermissionResponseDto;
import com.sabi.datacollection.core.models.AppCodes;
import com.sabi.datacollection.core.models.DataPermission;
import com.sabi.datacollection.service.helper.Validations;
import com.sabi.datacollection.service.repositories.DataPermissionRepository;
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
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class DataPermissionService {


    private final DataPermissionRepository dataPermissionRepository;
    private final ModelMapper mapper;
    private final Validations validations;
    private final AuditTrailService auditTrailService;

    public DataPermissionService(DataPermissionRepository dataPermissionRepository, ModelMapper mapper, Validations validations, AuditTrailService auditTrailService) {
        this.dataPermissionRepository = dataPermissionRepository;
        this.mapper = mapper;
        this.validations = validations;
        this.auditTrailService = auditTrailService;
    }

    public DataPermissionResponseDto createPermission(DataPermissionDto request, HttpServletRequest request1) {
        validations.validatePermission(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        DataPermission permission = mapper.map(request,DataPermission.class);

        DataPermission permissionExist = dataPermissionRepository.findByNameAndAppPermission(request.getName(),request.getAppPermission());
        if(permissionExist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Permission already exist");
        }

        permission.setCreatedBy(userCurrent.getId());
        permission.setStatus(CustomResponseCode.ACTIVE_USER);
        permission = dataPermissionRepository.save(permission);
        log.debug("Create new permission - {}"+ new Gson().toJson(permission));

        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Create new permission by :" + userCurrent.getUsername(),
                        AuditTrailFlag.CREATE,
                        " Create new permission for:" + permission.getName(),1, Utility.getClientIp(request1));
        return mapper.map(permission, DataPermissionResponseDto.class);
    }

    public DataPermissionResponseDto updatePermission(DataPermissionDto request,HttpServletRequest request1) {
//        coreValidations.validateFunction(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        DataPermission permission = dataPermissionRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested permission id does not exist!"));

        mapper.map(request, permission);
        permission.setUpdatedBy(userCurrent.getId());
        dataPermissionRepository.save(permission);
        log.debug("permission record updated - {}"+ new Gson().toJson(permission));

        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        "Update permission by username:" + userCurrent.getUsername(),
                        AuditTrailFlag.UPDATE,
                        " Update permission Request for:" + permission.getId(),1, Utility.getClientIp(request1));
        return mapper.map(permission, DataPermissionResponseDto.class);
    }

    public DataPermissionResponseDto findPermission(Long id){
        DataPermission permission = dataPermissionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested permission id does not exist!"));
        return mapper.map(permission,DataPermissionResponseDto.class);
    }

    public Page<DataPermission> findAll(String name, String appPermission, PageRequest pageRequest ){
        Page<DataPermission> functions = dataPermissionRepository.findFunctions(name,appPermission,pageRequest);
        if(functions == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return functions;

    }


    public List<DataPermission> getAll(String name, String appPermission){
        List<DataPermission> permissions = dataPermissionRepository.listPermission(name,appPermission);
        return permissions;

    }

    public List<AccessListDto> getPermissionsByUserId(Long userId) {

        List<AccessListDto> resultLists = new ArrayList<>();
        List<Object[]> result = dataPermissionRepository.getPermissionsByUserId(userId);
        try {
            result.forEach(r -> {
                AccessListDto userPermission = new AccessListDto();
                userPermission.setName((String) r[0]);
                userPermission.setAppPermission((String) r[1]);
                resultLists.add(userPermission);

            });
        } catch (Exception var5) {
            log.info("Error in returning object list" + var5);
        }
        return resultLists;

    }

}
