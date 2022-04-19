package com.sabi.datacollection.service.services;

import com.sabi.datacollection.core.dto.request.EnableDisEnableDto;
import com.sabi.datacollection.core.dto.request.DataRolePermissionDto;
import com.sabi.datacollection.core.dto.response.DataRolePermissionResponseDto;
import com.sabi.datacollection.core.models.DataPermission;
import com.sabi.datacollection.core.models.DataRolePermission;
import com.sabi.datacollection.service.repositories.DataPermissionRepository;
import com.sabi.datacollection.service.repositories.DataRolePermissionRepository;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
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

    private final DataRolePermissionRepository dataRolePermissionRepository;
    private final ModelMapper mapper;
    private final DataPermissionRepository dataPermissionRepository;


    public DataRolePermissionService(DataRolePermissionRepository DataRolePermissionRepository,
                                     ModelMapper mapper,
                                     DataPermissionRepository dataPermissionRepository) {
        this.dataRolePermissionRepository = DataRolePermissionRepository;
        this.mapper = mapper;
        this.dataPermissionRepository = dataPermissionRepository;
    }

    public void assignPermission(DataRolePermissionDto request) {
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        List<DataRolePermission> rolePerm = new ArrayList<>();
        DataRolePermission rolePermission = new DataRolePermission();
        request.getPermissionIds().forEach(p -> {
            rolePermission.setPermissionId(p.getPermissionId());
            rolePermission.setRoleId(request.getRoleId());
            rolePermission.setCreatedBy(userCurrent.getId());
            log.info(" role permission details " + rolePermission);
            DataRolePermission exist = dataRolePermissionRepository.findByRoleIdAndPermissionId(request.getRoleId(),p.getPermissionId());
            if(exist != null){
                throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Permission id already assigned to the role ::::"+p.getPermissionId());
            }
            dataRolePermissionRepository.save(rolePermission);
            rolePerm.add(rolePermission);

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
