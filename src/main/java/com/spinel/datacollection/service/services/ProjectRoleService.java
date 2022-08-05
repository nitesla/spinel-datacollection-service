package com.spinel.datacollection.service.services;


import com.spinel.datacollection.core.dto.request.EnableDisableDto;
import com.spinel.datacollection.core.dto.request.ProjectRoleDto;
import com.spinel.datacollection.core.dto.response.ProjectRoleResponseDto;
import com.spinel.datacollection.core.models.ProjectRole;
import com.spinel.datacollection.service.helper.Validations;
import com.spinel.datacollection.service.repositories.ProjectRoleRepository;

import com.spinel.framework.exceptions.ConflictException;
import com.spinel.framework.exceptions.NotFoundException;
import com.spinel.framework.models.User;
import com.spinel.framework.service.TokenService;
import com.spinel.framework.utils.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectRoleService {

    private final ProjectRoleRepository projectRoleRepository;
    private final ModelMapper mapper;
    private final Validations validations;

    public ProjectRoleResponseDto createProjectRole(ProjectRoleDto request) {
        validations.validateProjectRole(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        ProjectRole projectRole = mapper.map(request, ProjectRole.class);
        ProjectRole projectRoleExist = projectRoleRepository.findByName(request.getName());
        if(Objects.nonNull(projectRoleExist)) {
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "Project Role already exist");
        }
        projectRole.setCreatedBy(userCurrent.getId());
        projectRole.setIsActive(true);
        projectRoleRepository.save(projectRole);
        log.info("Created new Sector - {}", projectRole);
        return mapper.map(projectRole, ProjectRoleResponseDto.class);
    }

    public ProjectRoleResponseDto updateProjectRole(ProjectRoleDto request) {
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        ProjectRole projectRole = projectRoleRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Project Role Id does not exist!"));
        mapper.map(request, projectRole);
        projectRole.setUpdatedBy(userCurrent.getId());
        projectRoleRepository.save(projectRole);
        log.info("Project Role update - {}", projectRole);
        return mapper.map(projectRole, ProjectRoleResponseDto.class);
    }

    public ProjectRoleResponseDto findProjectRoleById(Long id){
        ProjectRole projectRole = projectRoleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Project Role Id does not exist!"));
        return mapper.map(projectRole, ProjectRoleResponseDto.class);
    }

    public Page<ProjectRole> findAll(String name, PageRequest pageRequest ) {
        Page<ProjectRole> projectRoles = projectRoleRepository.findProjectRoles(name, pageRequest);
        if (projectRoles == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return projectRoles;

    }

    public void enableDisableState (EnableDisableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        ProjectRole projectRole = projectRoleRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Project Role Id does not exist!"));
        projectRole.setIsActive(request.getIsActive());
        projectRole.setUpdatedBy(userCurrent.getId());
        projectRoleRepository.save(projectRole);
    }

    public List<ProjectRole> getAll(Boolean isActive){
        return projectRoleRepository.findByIsActive(isActive);
    }

}
