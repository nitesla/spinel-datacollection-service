package com.spinel.datacollection.service.services;



import com.spinel.datacollection.core.dto.request.EnableDisableDto;
import com.spinel.datacollection.core.dto.request.ProjectCategoryDto;
import com.spinel.datacollection.core.dto.response.ProjectCategoryResponseDto;
import com.spinel.datacollection.core.models.ProjectCategory;
import com.spinel.datacollection.service.helper.Validations;
import com.spinel.datacollection.service.repositories.ProjectCategoryRepository;
import com.spinel.datacollection.service.repositories.ProjectOwnerRepository;
import com.spinel.framework.exceptions.ConflictException;
import com.spinel.framework.exceptions.NotFoundException;
import com.spinel.framework.models.User;
import com.spinel.framework.service.TokenService;
import com.spinel.framework.utils.CustomResponseCode;
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


    private final ProjectOwnerRepository projectOwnerRepository;
    private final ProjectCategoryRepository projectCategoryRepository;
    private final ModelMapper mapper;
    private final Validations validations;

    public ProjectCategoryService(ProjectOwnerRepository projectOwnerRepository, ProjectCategoryRepository projectCategoryRepository, ModelMapper mapper, Validations validations) {
        this.projectOwnerRepository = projectOwnerRepository;
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

    public ProjectCategoryResponseDto findProjectCategoryById(Long id){
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

    public void enableDisableState (EnableDisableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        ProjectCategory projectCategory = projectCategoryRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested ProjectCategory Id does not exist!"));
        projectCategory.setIsActive(request.getIsActive());
        projectCategory.setUpdatedBy(userCurrent.getId());
        projectCategoryRepository.save(projectCategory);

    }

    public List<ProjectCategory> getAll(Boolean isActive){
        List<ProjectCategory> projectCategories = projectCategoryRepository.findByIsActive(isActive);
        return projectCategories;
    }

}
