package com.sabi.datacollection.service.services;

import com.sabi.datacollection.core.dto.request.EnableDisableDto;
import com.sabi.datacollection.core.dto.request.ProjectFormRequestDto;
import com.sabi.datacollection.core.dto.request.ProjectFormRequestDto;
import com.sabi.datacollection.core.dto.response.ProjectFormResponseDto;
import com.sabi.datacollection.core.dto.response.ProjectFormResponseDto;
import com.sabi.datacollection.core.dto.response.ProjectFormResponseDto;
import com.sabi.datacollection.core.models.ProjectForm;
import com.sabi.datacollection.core.models.ProjectForm;
import com.sabi.datacollection.core.models.ProjectForm;
import com.sabi.datacollection.core.models.ProjectForm;
import com.sabi.datacollection.service.helper.Validations;
import com.sabi.datacollection.service.repositories.ProjectFormRepository;
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
public class ProjectFormService {

    private final ProjectFormRepository projectFormRepository;

    private final Validations validations;

    private final ModelMapper modelMapper;

    public ProjectFormService(ProjectFormRepository projectFormRepository, Validations validations, ModelMapper modelMapper) {
        this.projectFormRepository = projectFormRepository;
        this.validations = validations;
        this.modelMapper = modelMapper;
    }

    /**
     Helper inner class
     */
    private ProjectForm setsAndSavesProjectForm(ProjectFormRequestDto projectFormRequestDto, User currentUser, String actionType){
        ProjectForm projectForm;
        if (actionType.equalsIgnoreCase("create"))
        {
            projectForm= projectFormRepository.findByProjectIdAndFormId(projectFormRequestDto.getProjectId(),projectFormRequestDto.getFormId());
            if(projectForm!=null){
                throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION,"This projectForm already exists");
            }
            projectForm = modelMapper.map(projectFormRequestDto,ProjectForm.class);
            projectForm.setCreatedBy(currentUser.getId());
            projectForm.setIsActive(true);
        }
        else {
            projectForm = projectFormRepository.findById(projectFormRequestDto.getId())
                    .orElseThrow(()->new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"Invalid ProjectForm Id"));
            modelMapper.map(projectFormRequestDto,projectForm);
            projectForm.setUpdatedBy(currentUser.getId());
        }
        projectForm= projectFormRepository.save(projectForm);
        return projectForm;

    }

    /**
     <summary>
     Creates project Form
     </summary>
     <remarks>This method creates an instance of project Form and returns the result</remarks>
     */
    public ProjectFormResponseDto createProjectForm(ProjectFormRequestDto projectFormRequestDto){
        validations.validateProjectForm(projectFormRequestDto);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        ProjectForm projectForm = this.setsAndSavesProjectForm(projectFormRequestDto,userCurrent,"create");
        log.info("Created new projectForm:{}",projectForm);
        return modelMapper.map(projectForm, ProjectFormResponseDto.class);

    }

    /**
     <summary>
     Updates list of project Forms
     </summary>
     <remarks>This method creates in bulk project Forms and returns the result</remarks>
     */
    public ProjectFormResponseDto updateProjectForm(ProjectFormRequestDto projectFormRequestDto){
        validations.validateProjectForm(projectFormRequestDto);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        ProjectForm projectForm = this.setsAndSavesProjectForm(projectFormRequestDto,userCurrent,"update");
        log.info("Updated  projectForm:{}",projectForm);
        return modelMapper.map(projectForm, ProjectFormResponseDto.class);
    }

    /**
     <summary>
     Creates list of project Forms
     </summary>
     <remarks>This method creates in bulk project Forms and returns the result</remarks>
     */
    public List<ProjectFormResponseDto> createsProjectFormsInBulk(List<ProjectFormRequestDto> projectFormRequestDtoList) {
        User currentUser = TokenService.getCurrentUserFromSecurityContext();
        projectFormRequestDtoList.forEach(this.validations::validateProjectForm);
        List<ProjectFormResponseDto> responseDtoList = new ArrayList<>();
        for (ProjectFormRequestDto projectFormRequestDto : projectFormRequestDtoList){
            responseDtoList.add(modelMapper.map(this.setsAndSavesProjectForm(projectFormRequestDto,currentUser,"create"),ProjectFormResponseDto.class));
        }
        return responseDtoList;
    }

    /**
     <summary>
     Updates list of project Forms
     </summary>
     <remarks>This method updates in bulk project Forms and returns the result</remarks>
     */
    public List<ProjectFormResponseDto> updatesProjectFormsInBulk(List<ProjectFormRequestDto> projectFormRequestDtoList) {
        User currentUser = TokenService.getCurrentUserFromSecurityContext();
        for (ProjectFormRequestDto projectFormRequestDto : projectFormRequestDtoList){
            this.validations.validateProjectForm(projectFormRequestDto);
        }
        List<ProjectFormResponseDto> responseDtoList = new ArrayList<>();
        for (ProjectFormRequestDto projectFormRequestDto : projectFormRequestDtoList){
            responseDtoList.add(modelMapper.map(this.setsAndSavesProjectForm(projectFormRequestDto,currentUser,"update"),ProjectFormResponseDto.class));
        }
        return responseDtoList;
    }

    /**
     <summary>
     get Project Form
     </summary>
     <remarks>This method gets a single instance of project Form.</remarks>
     */
    public ProjectFormResponseDto getProjectForm(Long id){
        ProjectForm projectForm = projectFormRepository.findById(id)
                .orElseThrow(()->new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"The requested ProjectForm does not exist"));
        return modelMapper.map(projectForm, ProjectFormResponseDto.class);
    }

    /**
     <summary>
     search All
     </summary>
     <remarks>This method searches for all project Forms and returns pagination</remarks>
     */
    public Page<ProjectForm> searchAll(Long projectId, Long FormId, PageRequest pageRequest){
        Page<ProjectForm> projectForms = projectFormRepository.searchProjectForms(projectId,FormId,pageRequest);
        if (projectForms ==null)
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"No records found");
        return projectForms;
    }

    /**
     <summary>
     get Active project Forms
     </summary>
     <remarks>This method searches for all active project Forms and returns pagination</remarks>
     */
    public List<ProjectForm> getActiveForms(Boolean isActive){
        return projectFormRepository.findAllByIsActive(isActive);
    }

    /**
     <summary>
     Enables/Disables project Form
     </summary>
     <remarks>This method enables/disables a particular project Form </remark>
     */
    public void enableDisableState(EnableDisableDto enableDisableDto) {
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        ProjectForm projectForm = projectFormRepository.findById(enableDisableDto.getId())
                .orElseThrow(()->new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"The requested ProjectForm Id doesn't exist"));
        projectForm.setUpdatedBy(userCurrent.getId());
        projectForm.setIsActive(enableDisableDto.getIsActive());
        projectFormRepository.save(projectForm);

    }




}
