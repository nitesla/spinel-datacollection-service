package com.sabi.datacollection.service.services;

import com.sabi.datacollection.core.dto.request.EnableDisableDto;
import com.sabi.datacollection.core.dto.request.ProjectDto;
import com.sabi.datacollection.core.dto.response.ProjectResponseDto;
import com.sabi.datacollection.core.enums.Status;
import com.sabi.datacollection.core.models.Project;
import com.sabi.datacollection.core.models.ProjectCategory;
import com.sabi.datacollection.core.models.ProjectOwner;
import com.sabi.datacollection.service.helper.Validations;
import com.sabi.datacollection.service.repositories.ProjectCategoryRepository;
import com.sabi.datacollection.service.repositories.ProjectOwnerRepository;
import com.sabi.datacollection.service.repositories.ProjectRepository;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@SuppressWarnings("ALL")
@Slf4j
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectOwnerRepository projectOwnerRepository;
    private final ProjectCategoryRepository projectCategoryRepository;
    private final ModelMapper mapper;
    private final Validations validations;

    public ProjectService(ProjectRepository projectRepository, ProjectOwnerRepository projectOwnerRepository, ProjectCategoryRepository projectCategoryRepository, ModelMapper mapper, Validations validations) {
        this.projectRepository = projectRepository;
        this.projectOwnerRepository = projectOwnerRepository;
        this.projectCategoryRepository = projectCategoryRepository;
        this.mapper = mapper;
        this.validations = validations;
    }

    public ProjectResponseDto createProject(ProjectDto request) {
        validations.validateProject(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Project projectExists = projectRepository.findByName(request.getName());
        if(projectExists != null) {
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "Project already exists");
        }
        DateTimeFormatter format = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm");
        LocalDateTime startDate = LocalDateTime.parse(request.getStartDate(), format);
        LocalDateTime endDate = LocalDateTime.parse(request.getEndDate(), format);
        Project project = Project.builder()
                .name(request.getName())
                .projectCategoryId(request.getProjectCategoryId())
                .status(Status.valueOf(request.getStatus()))
                .startDate(startDate)
                .endDate(endDate)
                .sectorId(request.getSectorId())
                .imageQuality(request.getImageQuality())
                .isLocationBased(request.getIsLocationBased())
                .projectOwnerId(request.getProjectOwnerId())
                .build();
        project.setCreatedBy(userCurrent.getId());
        project.setIsActive(true);

        projectRepository.save(project);
        log.info("Created new Project  - {}", project);
        return mapper.map(project, ProjectResponseDto.class);
    }

    public ProjectResponseDto updateProject(ProjectDto request) {
        validations.validateProject(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Project project = projectRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Project Id does not exist!"));
        mapper.map(request, project);
        project.setUpdatedBy(userCurrent.getId());
        projectRepository.save(project);
        log.info("Project record updated - {}", project);
        return mapper.map(project, ProjectResponseDto.class);
    }

    public ProjectResponseDto findProjectById(Long id){
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Project Id does not exist!"));
        setProjectOwnerAndProjectCategory(project);
        return mapper.map(project, ProjectResponseDto.class);
    }

    public List<Project> findProjectByStatus(Status status) {
        List<Project> projects = projectRepository.findByStatus(status);
        if (projects == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        projects.forEach(project -> {
            setProjectOwnerAndProjectCategory(project);
        });
        return projects;
    }

    public Page<Project> findAll(String name, PageRequest pageRequest ) {
        Page<Project> projects = projectRepository.findProjects(name, pageRequest);
        if (projects == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        projects.getContent().forEach(project -> {
            setProjectOwnerAndProjectCategory(project);
        });
        return projects;

    }

    public void enableDisableState (EnableDisableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Project project = projectRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Project Id does not exist!"));
        project.setIsActive(request.getIsActive());
        project.setUpdatedBy(userCurrent.getId());
        projectRepository.save(project);

    }

    public List<Project> getAll(Boolean isActive){
        List<Project> projects = projectRepository.findByIsActive(isActive);
        projects.forEach(project -> {
            setProjectOwnerAndProjectCategory(project);
        });
        return projects;
    }

    private void setProjectOwnerAndProjectCategory(Project project) {
        ProjectOwner projectOwner = projectOwnerRepository.findById(project.getProjectOwnerId())
            .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                    "Requested Project Owner Id does not exist"));
        if(projectOwner.getLastname() != null && projectOwner.getLastname() != null){
            String projectOwnerName = projectOwner.getFirstname() + " " + projectOwner.getLastname();
            project.setProjectOwner(projectOwnerName);
        }
        ProjectCategory projectCategory = projectCategoryRepository.findById(project.getProjectCategoryId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Project Category Id does not exist"));
        project.setProjectCategory(projectCategory.getName());
    }
}
