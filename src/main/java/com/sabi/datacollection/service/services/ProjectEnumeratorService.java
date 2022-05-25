package com.sabi.datacollection.service.services;

import com.sabi.datacollection.core.dto.request.EnableDisableDto;
import com.sabi.datacollection.core.dto.request.ProjectEnumeratorRequestDto;
import com.sabi.datacollection.core.dto.response.ProjectEnumeratorResponseDto;
import com.sabi.datacollection.core.models.Enumerator;
import com.sabi.datacollection.core.models.Project;
import com.sabi.datacollection.core.models.ProjectEnumerator;
import com.sabi.datacollection.service.helper.Validations;
import com.sabi.datacollection.service.repositories.EnumeratorRepository;
import com.sabi.datacollection.service.repositories.ProjectEnumeratorRepository;
import com.sabi.datacollection.service.repositories.ProjectRepository;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.repositories.UserRepository;
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
public class ProjectEnumeratorService {

    private final ProjectEnumeratorRepository projectEnumeratorRepository;
    private final ProjectRepository projectRepository;
    private final EnumeratorRepository enumeratorRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final Validations validations;


    public ProjectEnumeratorService(ProjectEnumeratorRepository projectEnumeratorRepository, ProjectRepository projectRepository, EnumeratorRepository enumeratorRepository, UserRepository userRepository, ModelMapper modelMapper, Validations validations) {
        this.projectEnumeratorRepository = projectEnumeratorRepository;
        this.projectRepository = projectRepository;
        this.enumeratorRepository = enumeratorRepository;
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.validations = validations;
    }

    /**
     <summary>
     Creates project enumerator
     </summary>
     <remarks>This method creates an instance of project enumerator and returns the result</remarks>
     */
    public ProjectEnumeratorResponseDto createProjectEnumerator(ProjectEnumeratorRequestDto projectEnumeratorRequestDto){
        this.validations.validateProjectEnumerator(projectEnumeratorRequestDto);
        User currentUser = TokenService.getCurrentUserFromSecurityContext();
        ProjectEnumerator projectEnumerator= this.setsAndSavesProjectEnumerator(projectEnumeratorRequestDto,currentUser,"create");
        log.info("Created new projectEnumerator:{}",projectEnumerator);
        return modelMapper.map(projectEnumerator, ProjectEnumeratorResponseDto.class);
    }

    /**
     <summary>
     Creates list of project enumerators
     </summary>
     <remarks>This method creates in bulk project enumerators and returns the result</remarks>
     */
    public List<ProjectEnumeratorResponseDto> createsProjectEnumeratorsInBulk(List<ProjectEnumeratorRequestDto> projectEnumeratorRequestDtoList) {
        User currentUser = TokenService.getCurrentUserFromSecurityContext();
        for (ProjectEnumeratorRequestDto projectEnumeratorRequestDto : projectEnumeratorRequestDtoList){
            this.validations.validateProjectEnumerator(projectEnumeratorRequestDto);
        }
        List<ProjectEnumeratorResponseDto> responseDtoList = new ArrayList<>();
        for (ProjectEnumeratorRequestDto projectEnumeratorRequestDto : projectEnumeratorRequestDtoList){
            responseDtoList.add(modelMapper.map(this.setsAndSavesProjectEnumerator(projectEnumeratorRequestDto,currentUser,"create"),ProjectEnumeratorResponseDto.class));
        }
        return responseDtoList;
    }

    /**
     <summary>
     Updates list of project enumerators
     </summary>
     <remarks>This method updates in bulk project enumerators and returns the result</remarks>
     */
    public List<ProjectEnumeratorResponseDto> updatesProjectEnumeratorsInBulk(List<ProjectEnumeratorRequestDto> projectEnumeratorRequestDtoList) {
        User currentUser = TokenService.getCurrentUserFromSecurityContext();
        for (ProjectEnumeratorRequestDto projectEnumeratorRequestDto : projectEnumeratorRequestDtoList){
            this.validations.validateProjectEnumerator(projectEnumeratorRequestDto);
        }
        List<ProjectEnumeratorResponseDto> responseDtoList = new ArrayList<>();
        for (ProjectEnumeratorRequestDto projectEnumeratorRequestDto : projectEnumeratorRequestDtoList){
            responseDtoList.add(modelMapper.map(this.setsAndSavesProjectEnumerator(projectEnumeratorRequestDto,currentUser,"update"),ProjectEnumeratorResponseDto.class));
        }
        return responseDtoList;
    }

    /**
    Helper inner class
     */
    private ProjectEnumerator setsAndSavesProjectEnumerator(ProjectEnumeratorRequestDto projectEnumeratorRequestDto, User currentUser, String actionType){
        ProjectEnumerator projectEnumerator;
        if (actionType.equalsIgnoreCase("create"))
        {
            projectEnumerator= projectEnumeratorRepository.findByProjectIdAndEnumeratorId(projectEnumeratorRequestDto.getProjectId(),projectEnumeratorRequestDto.getEnumeratorId());
            if(projectEnumerator!=null){
                throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION,"This projectEnumerator already exists");
            }
            projectEnumerator = modelMapper.map(projectEnumeratorRequestDto,ProjectEnumerator.class);
            projectEnumerator.setCreatedBy(currentUser.getId());
            projectEnumerator.setIsActive(true);
        }
        else {
            projectEnumerator = projectEnumeratorRepository.findById(projectEnumeratorRequestDto.getId())
                    .orElseThrow(()->new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"Invalid ProjectEnumerator Id"));

            modelMapper.map(projectEnumeratorRequestDto,projectEnumerator);
            projectEnumerator.setUpdatedBy(currentUser.getId());
        }

        projectEnumerator= projectEnumeratorRepository.save(projectEnumerator);
        return projectEnumerator;

    }

    /**
     <summary>
     Updates project enumerator
     </summary>
     <remarks>This method updates an instance of project enumerator and returns the result</remarks>
     */
    public ProjectEnumeratorResponseDto updateProjectEnumerator(ProjectEnumeratorRequestDto projectEnumeratorRequestDto){
        this.validations.validateProjectEnumerator(projectEnumeratorRequestDto);
        User currentUser = TokenService.getCurrentUserFromSecurityContext();
        ProjectEnumerator projectEnumerator = this.setsAndSavesProjectEnumerator(projectEnumeratorRequestDto,currentUser,"update");
        log.info("ProjectEnumerator record updated:{}",projectEnumerator);
        return modelMapper.map(projectEnumerator, ProjectEnumeratorResponseDto.class);

    }
    /**
     <summary>
     get Project enumerator
     </summary>
     <remarks>This method gets a single instance of project enumerator.</remarks>
     */
    public ProjectEnumeratorResponseDto getProjectEnumerator(Long id){
       ProjectEnumerator projectEnumerator = projectEnumeratorRepository.findById(id)
                .orElseThrow(()->new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"The requested ProjectEnumerator does not exist"));
       setTransientField(projectEnumerator);
       return modelMapper.map(projectEnumerator, ProjectEnumeratorResponseDto.class);
    }
    /**
      <summary>
      search All
      </summary>
      <remarks>This method searches for all project enumerators and returns pagination</remarks>
     */
    public Page<ProjectEnumerator> searchAll(Long projectId, Long enumeratorId, PageRequest pageRequest){
        Page<ProjectEnumerator> projectEnumerators = projectEnumeratorRepository.findProjectEnumerators(projectId,enumeratorId,pageRequest);
        if (projectEnumerators ==null)
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"No records found");
        for (ProjectEnumerator projectEnumerator: projectEnumerators) {
            setTransientField(projectEnumerator);
        }
        return projectEnumerators;
    }

    /**
     <summary>
     get Active project Enumerators
     </summary>
     <remarks>This method searches for all active project enumerators and returns pagination</remarks>
     */
    public List<ProjectEnumerator> getActiveEnumerators(Boolean isActive){
        List<ProjectEnumerator> projectEnumerators = projectEnumeratorRepository.findAllByIsActive(isActive);
        for (ProjectEnumerator projectEnumerator: projectEnumerators) {
            setTransientField(projectEnumerator);
        }
        return projectEnumerators;
    }

    /**
     <summary>
     Enables/Disables project enumerator
     </summary>
     <remarks>This method enables/disables a particular project enumerator </remark>
     */
    public void enableDisableState(EnableDisableDto enableDisableDto) {
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        ProjectEnumerator projectEnumerator = projectEnumeratorRepository.findById(enableDisableDto.getId())
                .orElseThrow(()->new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"The requested ProjectEnumerator Id doesn't exist"));
        projectEnumerator.setUpdatedBy(userCurrent.getId());
        projectEnumerator.setIsActive(enableDisableDto.getIsActive());
        projectEnumeratorRepository.save(projectEnumerator);
    }

    private void setTransientField(ProjectEnumerator projectEnumerator){
        if(projectEnumerator.getProjectId() != null) {
            Project project = projectRepository.findById(projectEnumerator.getProjectId())
                    .orElseThrow(()->new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"The requested Project Id doesn't exist"));
            projectEnumerator.setDescription(project.getDescription());
        }
        if(projectEnumerator.getEnumeratorId() != null) {
            Enumerator enumerator = enumeratorRepository.findById(projectEnumerator.getEnumeratorId())
                    .orElseThrow(()->new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"The requested Enumerator Id doesn't exist"));
            User user = userRepository.findById(enumerator.getUserId())
                    .orElseThrow(()->new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,"The requested User Id doesn't exist"));

            projectEnumerator.setPhoneNumber(enumerator.getPhone());
            projectEnumerator.setFirstName(user.getFirstName());
            projectEnumerator.setLastName(user.getLastName());
            projectEnumerator.setLocation(enumerator.getAddress());
            projectEnumerator.setEmail(enumerator.getEmail());
        }

    }

}
