package com.sabi.datacollection.service.services;


import com.sabi.datacollection.core.dto.request.ProjectCategoryDto;
import com.sabi.datacollection.core.dto.response.ProjectCategoryResponseDto;
import com.sabi.datacollection.core.models.ProjectCategory;
import com.sabi.datacollection.service.helper.Validations;
import com.sabi.datacollection.service.repositories.ProjectCategoryRepository;
import com.sabi.framework.dto.requestDto.EnableDisEnableDto;
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

import java.util.List;

@SuppressWarnings("ALL")
@Slf4j
@Service
public class ProjectCategoryService {


    private final ProjectCategoryRepository projectCategoryRepository;
    private final ModelMapper mapper;
    private final Validations validations;

    public ProjectCategoryService(ProjectCategoryRepository projectCategoryRepository, ModelMapper mapper, Validations validations) {
        this.projectCategoryRepository = projectCategoryRepository;
        this.mapper = mapper;
        this.validations = validations;
    }

    public ProjectCategoryResponseDto createProjectCategory(ProjectCategoryDto request) {
        validations.validateProjectCategory(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        ProjectCategory projectCategory = mapper.map(request, ProjectCategory.class);
        ProjectCategory projectCategoryExist = projectCategoryRepository.findByName(request.getName());
        if(projectCategoryExist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Project Category already exist");
        }
        projectCategory.setCreatedBy(userCurrent.getId());
        projectCategory.setIsActive(true);
        projectCategoryRepository.save(projectCategory);
        log.info("Created new Project Category - {}", projectCategory);
        return mapper.map(projectCategory, ProjectCategoryResponseDto.class);
    }

    public ProjectCategoryResponseDto updateProjectCategory(ProjectCategoryDto request) {
        validations.validateProjectCategory(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        ProjectCategory projectCategory = projectCategoryRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Project Category Id does not exist!"));
        mapper.map(request, projectCategory);
        projectCategory.setUpdatedBy(userCurrent.getId());
        projectCategoryRepository.save(projectCategory);
        log.info("Project Category record updated - {}", projectCategory);
        return mapper.map(projectCategory, ProjectCategoryResponseDto.class);
    }

    public ProjectCategoryResponseDto findProjectCategory(Long id){
        ProjectCategory projectCategory = projectCategoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Project Category Id does not exist!"));
        return mapper.map(projectCategory, ProjectCategoryResponseDto.class);
    }


    public Page<ProjectCategory> findAll(String name, String description, PageRequest pageRequest ) {
        Page<ProjectCategory> projectCategories = projectCategoryRepository.findProjectCategories(name, description, pageRequest);
        if (projectCategories == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return projectCategories;

    }

    public void enableDisEnableState (EnableDisEnableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        ProjectCategory projectCategory = projectCategoryRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested ProjectCategory Id does not exist!"));
        projectCategory.setIsActive(request.isActive());
        projectCategory.setUpdatedBy(userCurrent.getId());
        projectCategoryRepository.save(projectCategory);

    }

    public List<ProjectCategory> getAll(Boolean isActive){
        return projectCategoryRepository.findByIsActive(isActive);
    }
}
