package com.sabi.datacollection.service.services;

import com.sabi.datacollection.core.dto.request.EnableDisableDto;
import com.sabi.datacollection.core.dto.request.ProjectOwnerEnumeratorDto;
import com.sabi.datacollection.core.dto.response.ProjectOwnerEnumeratorResponseDto;
import com.sabi.datacollection.core.models.ProjectOwnerEnumerator;
import com.sabi.datacollection.service.helper.Validations;
import com.sabi.datacollection.service.repositories.ProjectOwnerEnumeratorRepository;
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

@Slf4j
@Service
public class ProjectOwnerEnumeratorService {

    private final ModelMapper mapper;
    private final ProjectOwnerEnumeratorRepository projectOwnerEnumeratorRepository;
    private final Validations validations;


    public ProjectOwnerEnumeratorService(ModelMapper mapper, ProjectOwnerEnumeratorRepository projectOwnerEnumeratorRepository, Validations validations) {
        this.mapper = mapper;
        this.projectOwnerEnumeratorRepository = projectOwnerEnumeratorRepository;
        this.validations = validations;
    }

    public ProjectOwnerEnumeratorResponseDto saveProjectOwnerEnumerator(ProjectOwnerEnumeratorDto request) {

        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        ProjectOwnerEnumerator projectOwnerEnumerator = mapper.map(request, ProjectOwnerEnumerator.class);
        ProjectOwnerEnumerator projectOwnerEnumeratorExists = projectOwnerEnumeratorRepository.findProjectOwnerEnumeratorByProjectOwnerIdAndEnumeratorId(request.getProjectOwnerId(), request.getEnumeratorId());
        if(projectOwnerEnumeratorExists !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "Project Owner Enumerator already exist");
        }
        projectOwnerEnumerator.setCreatedBy(userCurrent.getId());
        projectOwnerEnumerator.setIsActive(true);
        projectOwnerEnumeratorRepository.save(projectOwnerEnumerator);
        log.info("Created new Project Owner Enumerator - {}", projectOwnerEnumerator);
        return mapper.map(projectOwnerEnumerator, ProjectOwnerEnumeratorResponseDto.class);
    }

    public ProjectOwnerEnumeratorResponseDto findProjectOwnerEnumeratorById(Long id){
        ProjectOwnerEnumerator projectOwnerEnumerator = projectOwnerEnumeratorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Project Owner Enumerator Id does not exist!"));
        return mapper.map(projectOwnerEnumerator, ProjectOwnerEnumeratorResponseDto.class);
    }

    public Page<ProjectOwnerEnumerator> findProjectOwnerEnumeratorByEnumeratorId(Long enumeratorId, Pageable pageable){
        Page<ProjectOwnerEnumerator> projectOwnerEnumeratorPage = projectOwnerEnumeratorRepository.findProjectOwnerEnumeratorByEnumeratorId(enumeratorId, pageable);
        if (projectOwnerEnumeratorPage == null ) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                    "Requested Project Owner Enumerator EnumeratorId does not exist!");
        }
        return projectOwnerEnumeratorPage;
    }

    public Page<ProjectOwnerEnumerator> findProjectOwnerEnumeratorByProjectOwnerId(Long projectOwnerId, Pageable pageable){
        Page<ProjectOwnerEnumerator> projectOwnerEnumeratorPage = projectOwnerEnumeratorRepository.findProjectOwnerEnumeratorByProjectOwnerId(projectOwnerId, pageable);
        if (projectOwnerEnumeratorPage == null ) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                    "Requested Project Owner Enumerator projectOwnerId does not exist!");
        }
        return projectOwnerEnumeratorPage;
    }

    public void enableDisableState (EnableDisableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        ProjectOwnerEnumerator projectOwnerEnumerator = projectOwnerEnumeratorRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Project Owner Enumerator Id does not exist!"));
        projectOwnerEnumerator.setIsActive(request.getIsActive());
        projectOwnerEnumerator.setUpdatedBy(userCurrent.getId());
        projectOwnerEnumeratorRepository.save(projectOwnerEnumerator);
    }

}
