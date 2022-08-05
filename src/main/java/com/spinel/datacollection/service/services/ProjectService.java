package com.spinel.datacollection.service.services;


import com.spinel.datacollection.core.dto.request.EnableDisableDto;
import com.spinel.datacollection.core.dto.request.ProjectDto;
import com.spinel.datacollection.core.dto.response.ProjectResponseDto;
import com.spinel.datacollection.core.enums.Status;
import com.spinel.datacollection.core.models.Project;
import com.spinel.datacollection.core.models.ProjectCategory;
import com.spinel.datacollection.core.models.ProjectOwner;
import com.spinel.datacollection.service.helper.Validations;
import com.spinel.datacollection.service.repositories.ProjectCategoryRepository;
import com.spinel.datacollection.service.repositories.ProjectOwnerRepository;
import com.spinel.datacollection.service.repositories.ProjectRepository;

import com.spinel.framework.exceptions.BadRequestException;
import com.spinel.framework.exceptions.ConflictException;
import com.spinel.framework.exceptions.NotFoundException;
import com.spinel.framework.models.AuditTrail;
import com.spinel.framework.models.User;
import com.spinel.framework.service.AuditTrailService;
import com.spinel.framework.service.TokenService;
import com.spinel.framework.utils.AuditTrailFlag;
import com.spinel.framework.utils.CustomResponseCode;
import com.spinel.framework.utils.Utility;
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
import java.util.Objects;

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

    public ProjectResponseDto createProject(ProjectDto request, HttpServletRequest request1) {
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
                .logEvent(project.getName(),
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
                .logEvent(project.getName(),
                        userCurrent.getUsername() + " updated " + project.getName(),
                        AuditTrailFlag.UPDATE,
                        "Update Project :" + project.getName(), 1, Utility.getClientIp(request1));
        return mapper.map(project, ProjectResponseDto.class);
    }

    public ProjectResponseDto findProjectById(Long id, HttpServletRequest request1) {
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Project Id does not exist!"));
        setTransientFields(project);
        auditTrailService
                .logEvent(project.getName(),
                        userCurrent.getUsername() + " viewed " + project.getName(),
                        AuditTrailFlag.VIEW,
                        "View Project :" + project.getName(), 1, Utility.getClientIp(request1));
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

    public Page<AuditTrail> getProjectAuditTrail(String projectName, String auditTrailFlag, PageRequest pageRequest) {
        String flag = null;
        if (Objects.nonNull(auditTrailFlag) && !EnumUtils.isValidEnum(AuditTrailFlag.class, auditTrailFlag.toUpperCase())) {
            throw new BadRequestException(CustomResponseCode.NOT_FOUND_EXCEPTION, "Enter a valid value for audittrail flag");
        }else {
            flag = auditTrailFlag;
        }
        return auditTrailService.findAll(projectName, null, flag, null, null, pageRequest);
    }

    private void setTransientFields(Project project) {
        if(project.getProjectOwnerId() != null) {
            ProjectOwner projectOwner = projectOwnerRepository.findById(project.getProjectOwnerId()).get();
            if(projectOwner.getFirstname() != null && projectOwner.getLastname() != null) {
                project.setProjectOwner(projectOwner.getFirstname() + " " + projectOwner.getLastname());
            }
            if(projectOwner != null) {
                project.setClientType(projectOwner.getIsCorp() ? "Corporate" : "Individual");
            }
        }

        if (project.getProjectCategoryId() != null) {
            ProjectCategory projectCategory = projectCategoryRepository.findById(project.getProjectCategoryId()).get();
            project.setProjectCategory(projectCategory.getName());
            project.setProjectCategoryDescription(projectCategory.getDescription());
        }
    }
}
