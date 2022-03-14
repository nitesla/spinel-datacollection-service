package com.sabi.datacollection.service.services;

import com.sabi.datacollection.core.dto.request.ProjectOwnerUserDto;
import com.sabi.datacollection.core.dto.request.EnableDisableDto;
import com.sabi.datacollection.core.dto.response.ProjectOwnerUserResponseDto;
import com.sabi.datacollection.core.models.ProjectOwnerUser;
import com.sabi.datacollection.service.helper.Validations;
import com.sabi.datacollection.service.repositories.ProjectOwnerUserRepository;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


@SuppressWarnings("ALL")
@Slf4j
@Service
public class ProjectOwnerUserService {

    private final ProjectOwnerUserRepository projectOwnerUserRepository;
    private final ModelMapper mapper;
    private final Validations validations;

    public ProjectOwnerUserService(ProjectOwnerUserRepository projectOwnerUserRepository, ModelMapper mapper, Validations validations) {
        this.projectOwnerUserRepository = projectOwnerUserRepository;
        this.mapper = mapper;
        this.validations = validations;
    }

    public ProjectOwnerUserResponseDto createProjectOwnerUser (ProjectOwnerUserDto request) {
        validations.validateProjectOwnerUser(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        ProjectOwnerUser projectOwnerUser = mapper.map(request, ProjectOwnerUser.class);
        ProjectOwnerUser projectOwnerUserExist = projectOwnerUserRepository.
                findProjectOwnerUserByUserIdAndProjectOwnerId(request.getUserId(), request.getProjectOwnerId());
        if(projectOwnerUserExist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "Project Owner User already exist");
        }
        projectOwnerUser.setCreatedBy(userCurrent.getId());
        projectOwnerUser.setIsActive(true);
        projectOwnerUserRepository.save(projectOwnerUser);
        log.info("Created new Project Owner User - {}", projectOwnerUser);
        return mapper.map(projectOwnerUser, ProjectOwnerUserResponseDto.class);
    }

    public ProjectOwnerUserResponseDto findProjectOwnerUserById(Long id){
        ProjectOwnerUser ProjectOwnerUser = projectOwnerUserRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Project Owner User Id does not exist!"));
        return mapper.map(ProjectOwnerUser, ProjectOwnerUserResponseDto.class);
    }

    public Page<ProjectOwnerUser> findProjectOwnerUserByUserId(Long userId, Pageable pageable){
        Page<ProjectOwnerUser> projectOwnerUserPage = projectOwnerUserRepository.findProjectOwnerUserByUserId(userId, pageable);
        if (projectOwnerUserPage == null ) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                    "Requested Project Owner User userId does not exist!");
        }
        return projectOwnerUserPage;
    }

    public Page<ProjectOwnerUser> findProjectOwnerUserByProjectOwnerId(Long projectOwnerId, Pageable pageable){
        Page<ProjectOwnerUser> projectOwnerUserPage = projectOwnerUserRepository.findProjectOwnerUserByProjectOwnerId(projectOwnerId, pageable);
        if (projectOwnerUserPage == null ) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                    "Requested Project Owner User projectOwnerId does not exist!");
        }
        return projectOwnerUserPage;
    }

    public void enableDisableState (EnableDisableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        ProjectOwnerUser projectOwnerUser = projectOwnerUserRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Project Owner User Id does not exist!"));
        projectOwnerUser.setIsActive(request.getIsActive());
        projectOwnerUser.setUpdatedBy(userCurrent.getId());
        projectOwnerUserRepository.save(projectOwnerUser);
    }

}
