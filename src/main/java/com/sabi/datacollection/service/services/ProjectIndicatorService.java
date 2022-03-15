package com.sabi.datacollection.service.services;

import com.sabi.datacollection.core.dto.request.EnableDisableDto;
import com.sabi.datacollection.core.dto.request.ProjectIndicatorDto;
import com.sabi.datacollection.core.dto.response.ProjectIndicatorResponseDto;
import com.sabi.datacollection.core.models.ProjectIndicator;
import com.sabi.datacollection.service.helper.Validations;
import com.sabi.datacollection.service.repositories.ProjectIndicatorRepository;
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
@Service
@Slf4j
public class ProjectIndicatorService {

    private final ProjectIndicatorRepository projectIndicatorRepository;
    private final ModelMapper mapper;
    private final Validations validations;

    public ProjectIndicatorService(ProjectIndicatorRepository projectIndicatorRepository, ModelMapper mapper, Validations validations) {
        this.projectIndicatorRepository = projectIndicatorRepository;
        this.mapper = mapper;
        this.validations = validations;
    }

    public ProjectIndicatorResponseDto createProjectIndicator (ProjectIndicatorDto request) {
        validations.validateProjectIndicator(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        ProjectIndicator projectIndicator = mapper.map(request, ProjectIndicator.class);
        ProjectIndicator projectIndicatorExist = projectIndicatorRepository.
                findProjectIndicatorByProjectIdAndIndicatorId(request.getProjectId(), request.getIndicatorId());
        if(projectIndicatorExist != null) {
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "Project Indicator already exist");
        }
        projectIndicator.setCreatedBy(userCurrent.getId());
        projectIndicator.setIsActive(true);
        projectIndicatorRepository.save(projectIndicator);
        log.info("Created Project Indicator - {}", projectIndicator);
        return mapper.map(projectIndicator, ProjectIndicatorResponseDto.class);
    }

    public ProjectIndicatorResponseDto updateProjectIndicator(ProjectIndicatorDto request) {
        validations.validateProjectIndicator(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        ProjectIndicator projectIndicator = projectIndicatorRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Enter a valid Project Indicator Id"));
        mapper.map(request, projectIndicator);
        projectIndicator.setCreatedBy(userCurrent.getId());
        projectIndicatorRepository.save(projectIndicator);
        log.info("Updated Project Indicator", projectIndicator);
        return mapper.map(projectIndicator, ProjectIndicatorResponseDto.class);
    }

    public ProjectIndicatorResponseDto findProjectIndicatorById(Long id){
        ProjectIndicator projectIndicator = projectIndicatorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Project Indicator Id does not exist!"));
        return mapper.map(projectIndicator, ProjectIndicatorResponseDto.class);
    }

    public Page<ProjectIndicator> findProjectIndicatorByProjectId(Long projectId, Pageable pageable){
        Page<ProjectIndicator> projectIndicatorPage = projectIndicatorRepository.findProjectIndicatorByProjectId(projectId, pageable);
        if (projectIndicatorPage == null ) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                    "Requested Project Indicator projectId does not exist!");
        }
        return projectIndicatorPage;
    }

    public Page<ProjectIndicator> findProjectIndicatorByIndicatorId(Long indicatorId, Pageable pageable){
        Page<ProjectIndicator> projectIndicatorPage = projectIndicatorRepository.findProjectIndicatorByIndicatorId(indicatorId, pageable);
        if (projectIndicatorPage == null ) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                    "Requested Project Indicator indicatorId does not exist!");
        }
        return projectIndicatorPage;
    }

    public Page<ProjectIndicator> findAll(Pageable pageable){
        Page<ProjectIndicator> projectIndicatorPage = projectIndicatorRepository.findAll( pageable);
        if (projectIndicatorPage == null ) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                    "Requested Project Indicator indicatorId does not exist!");
        }
        return projectIndicatorPage;
    }

    public Page<ProjectIndicator> findAllIsActive(Boolean isActive, Pageable pageable){
        Page<ProjectIndicator> projectIndicatorPage = projectIndicatorRepository.findProjectIndicatorByIsActive(isActive, pageable);
        if (projectIndicatorPage == null ) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                    "Requested Project Indicator indicatorId does not exist!");
        }
        return projectIndicatorPage;
    }

    public void enableDisableState (EnableDisableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        ProjectIndicator projectIndicator = projectIndicatorRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Project Indicator Id does not exist!"));
        projectIndicator.setIsActive(request.getIsActive());
        projectIndicator.setUpdatedBy(userCurrent.getId());
        projectIndicatorRepository.save(projectIndicator);
    }
}
