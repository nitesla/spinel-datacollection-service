package com.sabi.datacollection.service.services;

import com.sabi.datacollection.core.dto.request.EnableDisableDto;
import com.sabi.datacollection.core.dto.request.ProjectBillingRequestDto;
import com.sabi.datacollection.core.dto.request.ProjectBillingRequestDto;
import com.sabi.datacollection.core.dto.request.ProjectBillingRequestDto;
import com.sabi.datacollection.core.dto.response.ProjectBillingResponseDto;
import com.sabi.datacollection.core.dto.response.ProjectBillingResponseDto;
import com.sabi.datacollection.core.dto.response.ProjectBillingResponseDto;
import com.sabi.datacollection.core.dto.response.ProjectBillingResponseDto;
import com.sabi.datacollection.core.models.ProjectBilling;
import com.sabi.datacollection.core.models.ProjectBilling;
import com.sabi.datacollection.core.models.ProjectBilling;
import com.sabi.datacollection.service.helper.Validations;
import com.sabi.datacollection.service.repositories.ProjectBillingRepository;
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

@Service
@Slf4j
public class ProjectBillingService {
    private final ProjectBillingRepository projectBillingRepository;
    private final Validations validations;
    private final ModelMapper modelMapper;

    public ProjectBillingService(ProjectBillingRepository projectBillingRepository, Validations validations, ModelMapper modelMapper) {
        this.projectBillingRepository = projectBillingRepository;
        this.validations = validations;
        this.modelMapper = modelMapper;
    }

    /**
     Helper inner class
     */
    private ProjectBilling setsAndSavesProjectBilling(ProjectBillingRequestDto projectBillingRequestDto, User currentUser, String actionType){
        ProjectBilling projectBilling;
        if (actionType.equalsIgnoreCase("create"))
        {
            projectBilling= projectBillingRepository.findByProjectId(projectBillingRequestDto.getProjectId());
            if(projectBilling!=null){
                throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION,"This projectBilling already exists");
            }
            projectBilling = modelMapper.map(projectBillingRequestDto,ProjectBilling.class);
            projectBilling.setCreatedBy(currentUser.getId());
            projectBilling.setIsActive(true);
        }
        else {
            projectBilling = projectBillingRepository.findById(projectBillingRequestDto.getId())
                    .orElseThrow(()->new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"Invalid ProjectBilling Id"));

            modelMapper.map(projectBillingRequestDto,projectBilling);
            projectBilling.setUpdatedBy(currentUser.getId());
        }
        projectBilling= projectBillingRepository.save(projectBilling);
        return projectBilling;

    }

    /**
     <summary>
     Creates project Billing
     </summary>
     <remarks>This method creates an instance of project Billing and returns the result</remarks>
     */
    public ProjectBillingResponseDto createProjectBilling(ProjectBillingRequestDto projectBillingRequestDto){
        this.validations.validateProjectBilling(projectBillingRequestDto);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        ProjectBilling projectBilling = this.setsAndSavesProjectBilling(projectBillingRequestDto,userCurrent,"create");
        log.info("Created new projectBilling:{}",projectBilling);
        return modelMapper.map(projectBilling, ProjectBillingResponseDto.class);
    }

    /**
     <summary>
     Updates list of project Billings
     </summary>
     <remarks>This method creates in bulk project Billings and returns the result</remarks>
     */
    public ProjectBillingResponseDto updateProjectBilling(ProjectBillingRequestDto projectBillingRequestDto){
        validations.validateProjectBilling(projectBillingRequestDto);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        ProjectBilling projectBilling = this.setsAndSavesProjectBilling(projectBillingRequestDto,userCurrent,"update");
        log.info("Updated  projectBilling:{}",projectBilling);
        return modelMapper.map(projectBilling, ProjectBillingResponseDto.class);
    }

    /**
     <summary>
     Creates list of project Billings
     </summary>
     <remarks>This method creates in bulk project Billings and returns the result</remarks>
     */
    public List<ProjectBillingResponseDto> createsProjectBillingsInBulk(List<ProjectBillingRequestDto> projectBillingRequestDtoList) {
        User currentUser = TokenService.getCurrentUserFromSecurityContext();
        projectBillingRequestDtoList.forEach(this.validations::validateProjectBilling);
        List<ProjectBillingResponseDto> responseDtoList = new ArrayList<>();
        for (ProjectBillingRequestDto projectBillingRequestDto : projectBillingRequestDtoList){
            responseDtoList.add(modelMapper.map(this.setsAndSavesProjectBilling(projectBillingRequestDto,currentUser,"create"),ProjectBillingResponseDto.class));
        }
        return responseDtoList;
    }

    /**
     <summary>
     Updates list of project Billings
     </summary>
     <remarks>This method updates in bulk project Billings and returns the result</remarks>
     */
    public List<ProjectBillingResponseDto> updatesProjectBillingsInBulk(List<ProjectBillingRequestDto> projectBillingRequestDtoList) {
        User currentUser = TokenService.getCurrentUserFromSecurityContext();
        for (ProjectBillingRequestDto projectBillingRequestDto : projectBillingRequestDtoList){
            this.validations.validateProjectBilling(projectBillingRequestDto);
        }
        List<ProjectBillingResponseDto> responseDtoList = new ArrayList<>();
        for (ProjectBillingRequestDto projectBillingRequestDto : projectBillingRequestDtoList){
            responseDtoList.add(modelMapper.map(this.setsAndSavesProjectBilling(projectBillingRequestDto,currentUser,"update"),ProjectBillingResponseDto.class));
        }
        return responseDtoList;
    }

    /**
     <summary>
     get Project Billing
     </summary>
     <remarks>This method gets a single instance of project Billing.</remarks>
     */
    public ProjectBillingResponseDto getProjectBilling(Long id){
        ProjectBilling projectBilling = projectBillingRepository.findById(id)
                .orElseThrow(()->new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"The requested ProjectBilling does not exist"));
        return modelMapper.map(projectBilling, ProjectBillingResponseDto.class);
    }

    /**
     <summary>
     search All
     </summary>
     <remarks>This method searches for all project Billings and returns pagination</remarks>
     */
    public Page<ProjectBilling> searchAll(Long projectId, PageRequest pageRequest){
        Page<ProjectBilling> projectBillings = projectBillingRepository.findProjectBillings(projectId,pageRequest);
        if (projectBillings ==null)
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"No records found");
        return projectBillings;
    }

    /**
     <summary>
     get Active project Billings
     </summary>
     <remarks>This method searches for all active project Billings and returns pagination</remarks>
     */
    public List<ProjectBilling> getActiveBillings(Boolean isActive){
        return projectBillingRepository.findAllByIsActive(isActive);
    }

    /**
     <summary>
     Enables/Disables project Billing
     </summary>
     <remarks>This method enables/disables a particular project Billing </remark>
     */
    public void enableDisableState(EnableDisableDto enableDisableDto) {
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        ProjectBilling projectBilling = projectBillingRepository.findById(enableDisableDto.getId())
                .orElseThrow(()->new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"The requested ProjectBilling Id doesn't exist"));
        projectBilling.setUpdatedBy(userCurrent.getId());
        projectBilling.setIsActive(enableDisableDto.getIsActive());
        projectBillingRepository.save(projectBilling);

    }


}
