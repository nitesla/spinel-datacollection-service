package com.sabi.datacollection.service.services;

import com.sabi.datacollection.core.dto.request.EnableDisableDto;
import com.sabi.datacollection.core.dto.request.ProjectDto;
import com.sabi.datacollection.core.dto.response.ProjectResponseDto;
import com.sabi.datacollection.core.enums.Gender;
import com.sabi.datacollection.core.enums.Status;
import com.sabi.datacollection.core.models.Project;
import com.sabi.datacollection.core.models.ProjectCategory;
import com.sabi.datacollection.core.models.ProjectOwner;
import com.sabi.datacollection.service.helper.Validations;
import com.sabi.datacollection.service.repositories.ProjectCategoryRepository;
import com.sabi.datacollection.service.repositories.ProjectOwnerRepository;
import com.sabi.datacollection.service.repositories.ProjectRepository;
import com.sabi.framework.exceptions.BadRequestException;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.AuditTrail;
import com.sabi.framework.models.User;
import com.sabi.framework.service.AuditTrailService;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.AuditTrailFlag;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.framework.utils.Utility;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
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
    private final AuditTrailService auditTrailService;
    private final ModelMapper mapper;
    private final Validations validations;

    public ProjectService(ProjectRepository projectRepository, ProjectOwnerRepository projectOwnerRepository, ProjectCategoryRepository projectCategoryRepository, AuditTrailService auditTrailService, ModelMapper mapper, Validations validations) {
        this.projectRepository = projectRepository;
        this.projectOwnerRepository = projectOwnerRepository;
        this.projectCategoryRepository = projectCategoryRepository;
        this.auditTrailService = auditTrailService;
        this.mapper = mapper;
        this.validations = validations;
    }

    public ProjectResponseDto createProject(ProjectDto request,  HttpServletRequest request1) {
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
        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        userCurrent.getUsername() + " created " + project.getName(),
                AuditTrailFlag.SIGNUP,
                "Create Project :" + project.getName(), 1, Utility.getClientIp(request1));


        return mapper.map(project, ProjectResponseDto.class);
    }

    public ProjectResponseDto updateProject(ProjectDto request, HttpServletRequest request1) {
        validations.validateProject(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Project project = projectRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Project Id does not exist!"));
        mapper.map(request, project);
        project.setUpdatedBy(userCurrent.getId());
        projectRepository.save(project);
        log.info("Project record updated - {}", project);
        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        userCurrent.getUsername() + " updated " + project.getName(),
                        AuditTrailFlag.UPDATE,
                        "Create Project :" + project.getName(), 1, Utility.getClientIp(request1));
        return mapper.map(project, ProjectResponseDto.class);
    }

    public ProjectResponseDto findProjectById(Long id, HttpServletRequest request1) {
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Project Id does not exist!"));
        setTransientFields(project);
        auditTrailService
                .logEvent(userCurrent.getUsername(),
                        userCurrent.getUsername() + " viewed " + project.getName(),
                        AuditTrailFlag.VIEW,
                        "Create Project :" + project.getName(), 1, Utility.getClientIp(request1));
        return mapper.map(project, ProjectResponseDto.class);
    }

    public List<Project> findProjectByStatus(Status status) {
        List<Project> projects = projectRepository.findByStatus(status);
        if (projects == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        projects.forEach(project -> {
            setTransientFields(project);
            project.setProjectCount(projects.size());
        });
        return projects;
    }

    public List<Project> findProjectByStatusAndCategory(String status, Long categoryId) {
        validations.validateProjectStatus(status);
        List<Project> projects = projectRepository.findByStatusAndProjectCategoryId(Status.valueOf(status), categoryId);
        if (projects == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        projects.forEach(project -> {
            setTransientFields(project);
        });
        return projects;
    }

    public List<Project> findProjectByCategory(Long categoryId) {
        List<Project> projects = projectRepository.findByProjectCategoryId(categoryId);
        if (projects == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        projects.forEach(project -> {
            setTransientFields(project);
            project.setProjectCount(projects.size());
        });
        return projects;
    }

    public Page<Project> findAll(String name, PageRequest pageRequest ) {
        Page<Project> projects = projectRepository.findProjects(name, pageRequest);
        if (projects == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        projects.getContent().forEach(project -> {
            setTransientFields(project);
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
            setTransientFields(project);
        });
        return projects;
    }

    public List<Project> findProjectByProjectOwner(Long projectOwnerId) {
        return projectRepository.findByProjectOwnerId(projectOwnerId);
    }

    public Page<AuditTrail> getProjectAuditTrail(String username, String projectName, String auditTrailFlag, PageRequest pageRequest) {
        String event = username + " created " + projectName;
        if (!EnumUtils.isValidEnum(AuditTrailFlag.class, auditTrailFlag.toUpperCase()))
            throw new BadRequestException(CustomResponseCode.NOT_FOUND_EXCEPTION, "Enter a valid value for audittrail flag");
        return auditTrailService.findAll(username, event, String.valueOf(auditTrailFlag), null, null, pageRequest);
    }

    private void setTransientFields(Project project) {
        if(project.getProjectOwnerId() != null) {
            ProjectOwner projectOwner = projectOwnerRepository.findById(project.getProjectOwnerId()).get();
            if(projectOwner.getFirstname() != null && projectOwner.getLastname() != null) {
                project.setProjectOwner(projectOwner.getFirstname() + " " + projectOwner.getLastname());
            }
            if(projectOwner != null) {
                project.setClientType(projectOwnerRepository.findById(projectOwner.getId()).get().getIsCorp() ? "Corporate" : "Individual");
            }
        }

        if (project.getProjectCategoryId() != null) {
            ProjectCategory projectCategory = projectCategoryRepository.findById(project.getProjectCategoryId()).get();
            project.setProjectCategory(projectCategory.getName());
            project.setProjectCategoryDescription(projectCategory.getDescription());
        }
    }
}
