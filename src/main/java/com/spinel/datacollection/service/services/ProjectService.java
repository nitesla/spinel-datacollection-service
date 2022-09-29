package com.spinel.datacollection.service.services;


import com.spinel.datacollection.core.dto.request.EnableDisableDto;
import com.spinel.datacollection.core.dto.request.GetRequestDto;
import com.spinel.datacollection.core.dto.request.ProjectDto;
import com.spinel.datacollection.core.dto.response.ProjectResponseDto;
import com.spinel.datacollection.core.enums.Status;
import com.spinel.datacollection.core.enums.SubmissionStatus;
import com.spinel.datacollection.core.models.*;
import com.spinel.datacollection.service.helper.GenericSpecification;
import com.spinel.datacollection.service.helper.SearchCriteria;
import com.spinel.datacollection.service.helper.SearchOperation;
import com.spinel.datacollection.service.helper.Validations;
import com.spinel.datacollection.service.repositories.*;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.*;

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
    private final SubmissionService submissionService;
    private final ProjectOwnerEnumeratorRepository projectOwnerEnumeratorRepository;
    private final ProjectFileRepository projectFileRepository;
    private final ProjectMediaRepository projectMediaRepository;
    private final ProjectProjectCategoryRepository projectProjectCategoryRepository;
    private final ProjectSurveyRepository projectSurveyRepository;

    public ProjectService(ProjectRepository projectRepository, ProjectOwnerRepository projectOwnerRepository, ProjectCategoryRepository projectCategoryRepository, AuditTrailService auditTrailService, ModelMapper mapper, Validations validations, SubmissionService submissionService, ProjectOwnerEnumeratorRepository projectOwnerEnumeratorRepository, ProjectFileRepository projectFileRepository, ProjectMediaRepository projectMediaRepository, ProjectProjectCategoryRepository projectProjectCategoryRepository, ProjectSurveyRepository projectSurveyRepository) {
        this.projectRepository = projectRepository;
        this.projectOwnerRepository = projectOwnerRepository;
        this.projectCategoryRepository = projectCategoryRepository;
        this.auditTrailService = auditTrailService;
        this.mapper = mapper;
        this.validations = validations;
        this.submissionService = submissionService;
        this.projectOwnerEnumeratorRepository = projectOwnerEnumeratorRepository;
        this.projectFileRepository = projectFileRepository;
        this.projectMediaRepository = projectMediaRepository;
        this.projectProjectCategoryRepository = projectProjectCategoryRepository;
        this.projectSurveyRepository = projectSurveyRepository;
    }

    public ProjectResponseDto createProject(ProjectDto request,  HttpServletRequest request1) {
        validations.validateProject(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Project projectExists = projectRepository.findByName(request.getName());
        if(projectExists != null) {
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "Project already exists");
        }

        List<Long> projectCategoryIds = request.getProjectCategoryIds();

        Project project = mapper.map(request, Project.class);
        project.setStatus(Status.valueOf(request.getStatus().toUpperCase()));
        project.setCreatedBy(userCurrent.getId());
        project.setIsActive(true);
        project.setProjectOwnerId(request.getProjectOwnerId());

        if(projectCategoryIds.size() > 0)
            project.setProjectCategory(projectCategoryRepository.findById(projectCategoryIds.get(0)).get().getName());
        if(projectCategoryIds.size() > 1)
            project.setProjectCategory2(projectCategoryRepository.findById(projectCategoryIds.get(1)).get().getName());
        if(projectCategoryIds.size() > 2)
            project.setProjectCategory3(projectCategoryRepository.findById(projectCategoryIds.get(2)).get().getName());


        Project savedProject = projectRepository.save(project);
        Long projectId = savedProject.getId();
        saveProjectFiles(projectId, request.getProjectFiles());
        saveProjectMedias(projectId, request.getProjectMedias());
        saveProjectCategoryIds(projectId, request.getProjectCategoryIds());
        saveProjectSurveys(projectId, request.getSurveys());


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

//    public List<Project> findProjectByStatusAndCategory(String status, Long categoryId) {
//        validations.validateProjectStatus(status);
//        List<Project> projects = projectRepository.findByStatusAndProjectCategoryId(Status.valueOf(status), categoryId);
//        if (projects == null) {
//            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
//        }
//        projects.forEach(project -> {
//            setTransientFields(project);
//        });
//        return projects;
//    }

//    public List<Project> findProjectByCategory(Long categoryId) {
//        List<Project> projects = projectRepository.findByProjectCategoryId(categoryId);
//        if (projects == null) {
//            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
//        }
//        projects.forEach(project -> {
//            setTransientFields(project);
//            project.setProjectCount(projects.size());
//        });
//        return projects;
//    }

    public Page<Project> findAll(String name, String status, PageRequest pageRequest ) {
        if(Objects.nonNull(status)) validations.validateProjectStatus(status.toUpperCase());
        Page<Project> projects = projectRepository.findProjects(name, Objects.nonNull(status) ? status.toUpperCase() : null, pageRequest);
        if (projects == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        projects.getContent().forEach(project -> {
            setTransientFields(project);
        });
        return projects;

    }

    public List<Project> findAll() {
//        if(Objects.nonNull(status)) validations.validateProjectStatus(status.toUpperCase());
        List<Project> projects = projectRepository.findAll();
        if (Objects.nonNull(projects)) {

            projects.forEach(project -> {
                setTransientFields(project);
            });
        }
        return projects;

    }

//    public List<Project> findProjectByCategory(Long categoryId) {
//        List<Project> projects = projectRepository.findByProjectCategoryId(categoryId);
//        if (projects == null) {
//            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
//        }
//        projects.forEach(project -> {
//            setTransientFields(project);
//            project.setProjectCount(projects.size());
//        });
//        return projects;
//    }



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
        Long projectId = project.getId();

        List<ProjectMedia> savedProjectMedias = projectMediaRepository.findByProjectId(projectId);
        List<String> projectMedias = new ArrayList<>(savedProjectMedias.size());
        savedProjectMedias.forEach(projectMedia -> {
            projectMedias.add(projectMedia.getMedia());
        });
        project.setProjectMedias(projectMedias);

        List<ProjectFile> savedProjectFiles = projectFileRepository.findByProjectId(projectId);
        List<String> projectFiles = new ArrayList<>(savedProjectFiles.size());
        savedProjectFiles.forEach(projectFile -> {
            projectFiles.add(projectFile.getFile());
        });
        project.setProjectFiles(projectFiles);

        List<ProjectSurvey> savedProjectSurveys = projectSurveyRepository.findByProjectId(projectId);
        List<String> projectSurveys = new ArrayList<>(savedProjectSurveys.size());
        savedProjectSurveys.forEach(projectSurvey -> {
            projectSurveys.add(projectSurvey.getSurvey());
        });
        project.setProjectSurveys(projectSurveys);
    }

    private int getDistinctCategories(Long projectOwnerId) {
        List<Project> projects = projectRepository.findByProjectOwnerId(projectOwnerId);
        Map<Long, String> categories = new HashMap<>();
        projects.forEach(project -> {
            List<ProjectProjectCategory> projectCategories = projectProjectCategoryRepository.findByProjectId(project.getId());
            projectCategories.forEach(projectCategory -> {
                Long projectCategoryId = projectCategory.getProjectCategoryId();
                categories.put(projectCategoryId, projectCategoryRepository.findById(projectCategoryId).get().getName());
            });
        });
        return categories.size();
    }


    public HashMap<String, Integer> getProjectSummary(Long projectOwnerId) {
        validateProjectOwner(projectOwnerId);

        int totalProjects = projectRepository.findByProjectOwnerId(projectOwnerId).size();
        int totalCategories = getDistinctCategories(projectOwnerId);
        int enumerators = projectOwnerEnumeratorRepository.findProjectOwnerEnumeratorByProjectOwnerId(projectOwnerId).size();;
        int submissions = submissionService.getSurveysForProject(projectRepository.findByProjectOwnerId(projectOwnerId), null);
        int activeProjects = projectRepository.findByProjectOwnerIdAndStatus(projectOwnerId, Status.ONGOING).size();
        int pendingProjects = projectRepository.findByProjectOwnerIdAndStatus(projectOwnerId, Status.INACTIVE).size();
        int pendingSurveys = submissionService.getSurveysForProject(projectRepository.findByProjectOwnerId(projectOwnerId), SubmissionStatus.INREVIEW);
        int drafts = projectRepository.findByProjectOwnerIdAndStatus(projectOwnerId, Status.DRAFT).size();

        return new HashMap<String, Integer>() {{
            put("totalProjects", totalProjects);
            put("totalCategories", totalCategories);
            put("enumerators", enumerators);
            put("submissions", submissions);
            put("activeProjects", activeProjects);
            put("pendingProjects", pendingProjects);
            put("pendingSurveys", pendingSurveys);
            put("drafts", drafts);
        }};
    }

    public Page<Project> getRecentProjects(Long projectOwnerId, int count) {
        validateProjectOwner(projectOwnerId);
        Sort sortType = Sort.by(Sort.Order.desc("id"));
        Pageable pageable = PageRequest.of(0, count, sortType);
        Page<Project> projects = projectRepository.findByProjectOwnerId(projectOwnerId, pageable);
        projects.forEach(project -> {
            setTransientFields(project);
        });
        return projects;
    }

    public Page<Project> getDrafts(Long projectOwnerId, int count) {
        validateProjectOwner(projectOwnerId);
        Sort sortType = Sort.by(Sort.Order.desc("id"));
        Pageable pageable = PageRequest.of(0, count, sortType);
        Page<Project> projects = projectRepository.findByProjectOwnerIdAndStatus(projectOwnerId, Status.DRAFT, pageable);
        projects.forEach(project -> {
            setTransientFields(project);
        });
        return projects;
    }

    private void validateProjectOwner(Long projectOwnerId){
        projectOwnerRepository.findById(projectOwnerId)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Project Owner Id does not exist!"));
    }

    private void saveProjectCategoryIds(Long projectId, List<Long> projectCategoryIds) {
        for (Long projectCategory : projectCategoryIds ) {
            ProjectProjectCategory projectProjectCategory = new ProjectProjectCategory();
            projectProjectCategory.setProjectId(projectId);
            projectProjectCategory.setProjectCategoryId(projectCategory);
            projectProjectCategoryRepository.save(projectProjectCategory);
        }
    }

    private void saveProjectSurveys(Long projectId, List<String> surveys ) {
        for (String survey : surveys ) {
            ProjectSurvey projectSurvey = new ProjectSurvey(projectId, survey);
            projectSurveyRepository.save(projectSurvey);
        }
    }

    private void saveProjectMedias(Long projectId, List<String> medias ) {
        for (String media : medias ) {
            ProjectMedia projectMedia = new ProjectMedia(projectId, media);
            projectMediaRepository.save(projectMedia);
        }
    }

    private void saveProjectFiles(Long projectId, List<String> files) {
        for (String file : files ) {
            ProjectFile projectFile = new ProjectFile(projectId, file);
            projectFileRepository.save(projectFile);
        }
    }

    public Page<Project> findFilteredPage(GetRequestDto request) {
        GenericSpecification<Project> genericSpecification = new GenericSpecification<Project>();

            if (request.getFilterCriteria() != null) {
                request.getFilterCriteria().forEach(filter -> {
                    if (filter.getFilterParameter() != null || filter.getFilterValue() != null) {
                        if (filter.getFilterParameter().equalsIgnoreCase("name")) {
                            genericSpecification.add(new SearchCriteria("name", filter.getFilterValue(), SearchOperation.MATCH));
                        }
                        if (filter.getFilterParameter().equalsIgnoreCase("status")) {
                            genericSpecification.add(new SearchCriteria("status", filter.getFilterValue(), SearchOperation.MATCH));
                        }
                        if (filter.getFilterParameter().equalsIgnoreCase("description")) {
                            genericSpecification.add(new SearchCriteria("description", filter.getFilterValue(), SearchOperation.MATCH));
                        }
                        if (filter.getFilterParameter().equalsIgnoreCase("isActive")) {
                            genericSpecification.add(new SearchCriteria("isActive", Boolean.valueOf(filter.getFilterValue()), SearchOperation.EQUAL));
                        }
                        if (filter.getFilterParameter().equalsIgnoreCase("isLocationBased")) {
                            genericSpecification.add(new SearchCriteria("isLocationBased", Boolean.valueOf(filter.getFilterValue()), SearchOperation.EQUAL));
                        }
                        if (filter.getFilterParameter().equalsIgnoreCase("enableTeams")) {
                            genericSpecification.add(new SearchCriteria("enableTeams", Boolean.valueOf(filter.getFilterValue()), SearchOperation.EQUAL));
                        }
                        if (filter.getFilterParameter().equalsIgnoreCase("enableGeoFencing")) {
                            genericSpecification.add(new SearchCriteria("enableGeoFencing", Boolean.valueOf(filter.getFilterValue()), SearchOperation.EQUAL));
                        }

                        if (filter.getFilterParameter().equalsIgnoreCase("enableEnumerators")) {
                            genericSpecification.add(new SearchCriteria("enableEnumerators", Boolean.valueOf(filter.getFilterValue()), SearchOperation.EQUAL));
                        }
                        if (filter.getFilterParameter().equalsIgnoreCase("enableAcceptanceCriteria")) {
                            genericSpecification.add(new SearchCriteria("enableAcceptanceCriteria", Boolean.valueOf(filter.getFilterValue()), SearchOperation.EQUAL));
                        }
                        if (filter.getFilterParameter().equalsIgnoreCase("isPublic")) {
                            genericSpecification.add(new SearchCriteria("isPublic", Boolean.valueOf(filter.getFilterValue()), SearchOperation.EQUAL));
                        }
                        if (filter.getFilterParameter().equalsIgnoreCase("isOpened")) {
                            genericSpecification.add(new SearchCriteria("isOpened", Boolean.valueOf(filter.getFilterValue()), SearchOperation.EQUAL));
                        }
                        if (filter.getFilterParameter().equalsIgnoreCase("projectCategory")) {
                            genericSpecification.add(new SearchCriteria("projectCategory", filter.getFilterValue(), SearchOperation.EQUAL));
                        }
                        if (filter.getFilterParameter().equalsIgnoreCase("projectCategory2")) {
                            genericSpecification.add(new SearchCriteria("projectCategory2", filter.getFilterValue(), SearchOperation.EQUAL));
                        }
                        if (filter.getFilterParameter().equalsIgnoreCase("projectCategory3")) {
                            genericSpecification.add(new SearchCriteria("projectCategory3", filter.getFilterValue(), SearchOperation.EQUAL));
                        }
                        if (filter.getFilterParameter().equalsIgnoreCase("id")) {
                            genericSpecification.add(new SearchCriteria("id", Long.parseLong(filter.getFilterValue()), SearchOperation.EQUAL));
                        }
                    }
                });
            }

            if (request.getFilterDate() != null) {

                request.getFilterDate().forEach(filter -> {
                    if (filter.getDateParameter() != null && filter.getDateParameter().equalsIgnoreCase("createdDate")) {
                        if (filter.getStartDate() != null) {
                            if (filter.getEndDate() != null && filter.getStartDate().isAfter(filter.getEndDate()))
                                throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "startDate can't be greater than endDate");
                            LocalDate startDate = LocalDate.parse(filter.getStartDate().toString());
                            genericSpecification.add(new SearchCriteria("createdDate", startDate, SearchOperation.GREATER_THAN_EQUAL));

                        }

                        if (filter.getEndDate() != null) {
                            if (filter.getStartDate() == null)
                                throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "'startDate' must be included along with 'endDate' in the request");
                            LocalDate endDate = LocalDate.parse(filter.getEndDate().toString());
                            genericSpecification.add(new SearchCriteria("createdDate", endDate, SearchOperation.LESS_THAN_EQUAL));

                        }
                    }
                });

            }

        if (request.getSortParameter() == null || request.getSortParameter().isEmpty()) {
            request.setSortDirection("desc");
            request.setSortParameter("id");
        }


        Sort sortType = (request.getSortDirection() != null && request.getSortDirection().equalsIgnoreCase("asc"))
                ?  Sort.by(Sort.Order.asc(request.getSortParameter())) :   Sort.by(Sort.Order.desc(request.getSortParameter()));

        PageRequest pageRequest = PageRequest.of(request.getPage(), request.getPageSize(), sortType);

        return projectRepository.findAll(genericSpecification, pageRequest);


    }

    public List<Project> findFilteredList(GetRequestDto request) {
        GenericSpecification<Project> genericSpecification = new GenericSpecification<Project>();
        if (request.getFilterCriteria() != null ) {
            request.getFilterCriteria().forEach(filter -> {
                if (filter.getFilterParameter() != null || filter.getFilterValue() != null) {
                    if (filter.getFilterParameter().equalsIgnoreCase("name")) {
                        genericSpecification.add(new SearchCriteria("name", filter.getFilterValue(), SearchOperation.MATCH));
                    }
                    if (filter.getFilterParameter().equalsIgnoreCase("status")) {
                        genericSpecification.add(new SearchCriteria("status", filter.getFilterValue(), SearchOperation.MATCH));
                    }
                    if (filter.getFilterParameter().equalsIgnoreCase("description")) {
                        genericSpecification.add(new SearchCriteria("description", filter.getFilterValue(), SearchOperation.MATCH));
                    }
                    if (filter.getFilterParameter().equalsIgnoreCase("isActive")) {
                        genericSpecification.add(new SearchCriteria("isActive", Boolean.valueOf(filter.getFilterValue()), SearchOperation.EQUAL));
                    }
                    if (filter.getFilterParameter().equalsIgnoreCase("isLocationBased")) {
                        genericSpecification.add(new SearchCriteria("isLocationBased", Boolean.valueOf(filter.getFilterValue()), SearchOperation.EQUAL));
                    }
                    if (filter.getFilterParameter().equalsIgnoreCase("enableTeams")) {
                        genericSpecification.add(new SearchCriteria("enableTeams", Boolean.valueOf(filter.getFilterValue()), SearchOperation.EQUAL));
                    }
                    if (filter.getFilterParameter().equalsIgnoreCase("enableGeoFencing")) {
                        genericSpecification.add(new SearchCriteria("enableGeoFencing", Boolean.valueOf(filter.getFilterValue()), SearchOperation.EQUAL));
                    }

                    if (filter.getFilterParameter().equalsIgnoreCase("enableEnumerators")) {
                        genericSpecification.add(new SearchCriteria("enableEnumerators", Boolean.valueOf(filter.getFilterValue()), SearchOperation.EQUAL));
                    }
                    if (filter.getFilterParameter().equalsIgnoreCase("enableAcceptanceCriteria")) {
                        genericSpecification.add(new SearchCriteria("enableAcceptanceCriteria", Boolean.valueOf(filter.getFilterValue()), SearchOperation.EQUAL));
                    }
                    if (filter.getFilterParameter().equalsIgnoreCase("isPublic")) {
                        genericSpecification.add(new SearchCriteria("isPublic", Boolean.valueOf(filter.getFilterValue()), SearchOperation.EQUAL));
                    }
                    if (filter.getFilterParameter().equalsIgnoreCase("isOpened")) {
                        genericSpecification.add(new SearchCriteria("isOpened", Boolean.valueOf(filter.getFilterValue()), SearchOperation.EQUAL));
                    }
                    if (filter.getFilterParameter().equalsIgnoreCase("projectCategory")) {
                        genericSpecification.add(new SearchCriteria("projectCategory", filter.getFilterValue(), SearchOperation.EQUAL));
                    }
                    if (filter.getFilterParameter().equalsIgnoreCase("projectCategory2")) {
                        genericSpecification.add(new SearchCriteria("projectCategory2", filter.getFilterValue(), SearchOperation.EQUAL));
                    }
                    if (filter.getFilterParameter().equalsIgnoreCase("projectCategory3")) {
                        genericSpecification.add(new SearchCriteria("projectCategory3", filter.getFilterValue(), SearchOperation.EQUAL));
                    }
                    if (filter.getFilterParameter().equalsIgnoreCase("id")) {
                        genericSpecification.add(new SearchCriteria("id", Long.parseLong(filter.getFilterValue()), SearchOperation.EQUAL));
                    }
                }
            });
        }
        if (request.getFilterDate() != null) {
            request.getFilterDate().forEach(filter -> {
                if (filter.getDateParameter() != null && filter.getDateParameter().equalsIgnoreCase("createdDate")) {
                    if (filter.getStartDate() != null) {
                        if (filter.getEndDate() != null && filter.getStartDate().isAfter(filter.getEndDate()))
                            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "startDate can't be greater than endDate");
                        LocalDate startDate = LocalDate.parse(filter.getStartDate().toString());
                        genericSpecification.add(new SearchCriteria("createdDate", startDate, SearchOperation.GREATER_THAN_EQUAL));

                    }

                    if (filter.getEndDate() != null) {
                        if (filter.getStartDate() == null)
                            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "'startDate' must be included along with 'endDate' in the request");
                        LocalDate endDate = LocalDate.parse(filter.getEndDate().toString());
                        genericSpecification.add(new SearchCriteria("createdDate", endDate, SearchOperation.LESS_THAN_EQUAL));

                    }
                }
            });
        }

        if (request.getSortParameter() == null || request.getSortParameter().isEmpty()) {
            request.setSortDirection("desc");
            request.setSortParameter("id");
        }

        Sort sortType = (request.getSortDirection() != null && request.getSortDirection().equalsIgnoreCase("asc"))
                ?  Sort.by(Sort.Order.asc(request.getSortParameter())) :   Sort.by(Sort.Order.desc(request.getSortParameter()));

        return projectRepository.findAll(genericSpecification, sortType);


    }

    public Page<Project> findUnfilteredPage(GetRequestDto request) {
        if (request.getSortParameter() == null || request.getSortParameter().isEmpty()) {
            request.setSortDirection("desc");
            request.setSortParameter("id");
        }
        Sort sortType = (request.getSortDirection() != null && request.getSortDirection().equalsIgnoreCase("asc"))
                ?  Sort.by(Sort.Order.asc(request.getSortParameter())) :   Sort.by(Sort.Order.desc(request.getSortParameter()));
        PageRequest pageRequest = PageRequest.of(request.getPage(), request.getPageSize(), sortType);
        return projectRepository.findAll(pageRequest);
    }

    public List<Project> findUnfilteredList() {
        return projectRepository.findAll();
    }

}
