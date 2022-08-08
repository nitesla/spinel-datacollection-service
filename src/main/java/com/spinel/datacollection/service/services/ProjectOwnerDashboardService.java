package com.spinel.datacollection.service.services;

import com.spinel.datacollection.core.enums.Status;
import com.spinel.datacollection.core.enums.SubmissionStatus;
import com.spinel.datacollection.core.models.Project;
import com.spinel.datacollection.service.helper.DateEnum;
import com.spinel.datacollection.service.repositories.ProjectOwnerEnumeratorRepository;
import com.spinel.datacollection.service.repositories.ProjectOwnerRepository;
import com.spinel.datacollection.service.repositories.ProjectRepository;
import com.spinel.framework.exceptions.NotFoundException;
import com.spinel.framework.utils.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@SuppressWarnings("ALL")
@Slf4j
@Service
public class ProjectOwnerDashboardService {

    private final ProjectRepository projectRepository;
    private final SubmissionService submissionService;
    private final ProjectOwnerRepository projectOwnerRepository;
    private final ProjectOwnerEnumeratorRepository projectOwnerEnumeratorRepository;

    public HashMap<String, Integer> dashboardInfo(Long projectOwnerId) {
        projectOwnerRepository.findById(projectOwnerId)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Project Owner does not exist!"));
        int allProjects = projectRepository.findByProjectOwnerId(projectOwnerId).size();
        int activeProjects = projectRepository.findByProjectOwnerIdAndStatus(projectOwnerId, Status.ONGOING).size();
        int completeProjects = projectRepository.findByProjectOwnerIdAndStatus(projectOwnerId, Status.COMPLETED).size();
        int cancelledProjects = projectRepository.findByProjectOwnerIdAndStatus(projectOwnerId, Status.SUSPENDED).size();
        int totalSubmissions = submissionService.getSurveysForProject(Objects.requireNonNull(projectRepository.findByProjectOwnerId(projectOwnerId)), null);
        int activeEnumerators = projectOwnerEnumeratorRepository.findProjectOwnerEnumeratorByProjectOwnerId(projectOwnerId).size();
        int pendingPayout = 0;
        int totalPayout = 0;

        return new HashMap<String, Integer>(){{
            put("allProjects", allProjects);
            put("activeProjects", activeProjects);
            put("completeProjects", completeProjects);
            put("cancelledProjects", cancelledProjects);
            put("totalSubmissions", totalSubmissions);
            put("activeEnumerators", activeEnumerators);
            put("pendingPayout", pendingPayout);
            put("totalPayout", totalPayout);
        }};
    }

    public HashMap<String, Integer> miniDashboardInfo(Long projectOwnerId) {
        projectOwnerRepository.findById(projectOwnerId)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Project Owner does not exist!"));
        int allProjects = projectRepository.findByProjectOwnerId(projectOwnerId).size();
        int enumerators = 0;
        int surveysInProgress = 0;
        int doneTasks = 0;

        return new HashMap<String, Integer>(){{
            put("allProjects", allProjects);
            put("enumerators", enumerators);
            put("surveysInProgress", surveysInProgress);
            put("doneTasks", doneTasks);
        }};
    }

    public HashMap<String, Integer> submissionSummary(Long projectOwnerId, int length, String dateType) {
        projectOwnerRepository.findById(projectOwnerId)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Project Owner does not exist!"));
        DateEnum.validateDateEnum(dateType);
        LocalDateTime startDate = LocalDateTime.now();
        HashMap<String, Integer> projects = null;

        if(DateEnum.MONTH.getValue().equals(dateType)) {
            LocalDateTime endDate = startDate.minusMonths(length);
            projects = submissionSummary(projectOwnerId, startDate, endDate);
        }
        if(DateEnum.WEEK.getValue().equals(dateType)) {
            LocalDateTime endDate = startDate.minusDays(length*7);
            projects = submissionSummary(projectOwnerId, startDate, endDate);
        }
        if(DateEnum.DAY.getValue().equals(dateType)) {
            LocalDateTime endDate = startDate.minusDays(length);
            projects = submissionSummary(projectOwnerId, startDate, endDate);
        }
        return projects;
    }

    private HashMap<String, Integer> submissionSummary(Long projectOwnerId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Project> projects = projectRepository.findByProjectOwnerIdAndCreatedDateBetween(projectOwnerId, startDate, endDate);
        int submissionsNeedingAuthentication = submissionService.getSurveysForProject(projects, SubmissionStatus.INREVIEW);
        int submissionsreturnedToEnumerator = submissionService.getSurveysForProject(projects, SubmissionStatus.REJECTED);
        int totalReturns = submissionService.getSurveysForProject(projects, null);

        return new HashMap<String, Integer>(){{
            put("submissionsNeedingAuthentication", submissionsNeedingAuthentication);
            put("submissionsreturnedToEnumerator", submissionsreturnedToEnumerator);
            put("totalReturns", totalReturns);
        }};
    }


}

