package com.spinel.datacollection.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.spinel.datacollection.core.dto.request.EnableDisableDto;
import com.spinel.datacollection.core.dto.request.SubmissionDto;
import com.spinel.datacollection.core.dto.response.SubmissionResponseDto;
import com.spinel.datacollection.core.enums.SubmissionStatus;
import com.spinel.datacollection.core.models.CommentDictionary;
import com.spinel.datacollection.core.models.Project;
import com.spinel.datacollection.core.models.ProjectEnumerator;
import com.spinel.datacollection.core.models.Submission;
import com.spinel.datacollection.service.helper.DateEnum;
import com.spinel.datacollection.service.helper.Validations;
import com.spinel.datacollection.service.repositories.CommentDictionaryRepository;
import com.spinel.datacollection.service.repositories.SubmissionRepository;
import com.spinel.framework.exceptions.BadRequestException;
import com.spinel.framework.exceptions.NotFoundException;
import com.spinel.framework.models.User;
import com.spinel.framework.service.TokenService;
import com.spinel.framework.utils.CustomResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 *
 * This class is responsible for all business logic for submission
 */


@Slf4j
@Service
public class SubmissionService {

    @Autowired
    private SubmissionRepository submissionRepository;
    @Autowired
    private CommentDictionaryRepository commentDictionaryRepository;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;
    private final Validations validations;

    public SubmissionService(ModelMapper mapper, ObjectMapper objectMapper, Validations validations) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.validations = validations;

    }

    /** <summary>
      * Submission creation
      * </summary>
      * <remarks>this method is responsible for creation of new submissions</remarks>
      */

    public SubmissionResponseDto createSubmission(SubmissionDto request) {
        validations.validateSubmission(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Submission submission = mapper.map(request,Submission.class);
//        Submission submissionExist = submissionRepository.findByName(request.getName());
//        if(submissionExist !=null){
//            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Submission already exist");
//        }
        submission.setCreatedBy(userCurrent.getId());
        submission.setIsActive(true);
        submission = submissionRepository.save(submission);
        log.debug("Create new Submission - {}"+ new Gson().toJson(submission));
        return mapper.map(submission, SubmissionResponseDto.class);
    }


    /** <summary>
     * Submission update
     * </summary>
     * <remarks>this method is responsible for updating already existing submissions</remarks>
     */

    public SubmissionResponseDto updateSubmission(SubmissionDto request) {
        validations.validateSubmission(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Submission submission = submissionRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Submission Id does not exist!"));
        mapper.map(request, submission);
        submission.setUpdatedBy(userCurrent.getId());
        submissionRepository.save(submission);
        log.debug("Submission record updated - {}"+ new Gson().toJson(submission));
        return mapper.map(submission, SubmissionResponseDto.class);
    }


    /** <summary>
     * Find Submission
     * </summary>
     * <remarks>this method is responsible for getting a single record</remarks>
     */
    public SubmissionResponseDto findSubmission(Long id){
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Submission Id does not exist!"));
        if (submission.getCommentId() != null){
            CommentDictionary commentDictionary = commentDictionaryRepository.getOne(submission.getCommentId());
            submission.setCommentName(commentDictionary.getName());
        }
        return mapper.map(submission,SubmissionResponseDto.class);
    }


    /** <summary>
     * Find all Submission
     * </summary>
     * <remarks>this method is responsible for getting all records in pagination</remarks>
     */
    public Page<Submission> findAll(Long projectId, Long formId, SubmissionStatus status, Long enumeratorId,  PageRequest pageRequest ){
        Page<Submission> submission = submissionRepository.findSubmissions(projectId, formId, status, enumeratorId, pageRequest);
            if(submission == null){
                throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
            }
        submission.getContent().forEach(submissions -> {
            CommentDictionary commentDictionary = commentDictionaryRepository.getOne(submissions.getCommentId());

            submissions.setCommentName(commentDictionary.getName());
        });
            return submission;
    }


    /** <summary>
     * Enable disenable
     * </summary>
     * <remarks>this method is responsible for enabling and dis enabling a submission</remarks>
     */
    public void enableDisEnableSubmission (EnableDisableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Submission submission = submissionRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Submission Id does not exist!"));
        submission.setIsActive(request.getIsActive());
        submission.setUpdatedBy(userCurrent.getId());
        submissionRepository.save(submission);

    }

    public List<Submission> getAll(Boolean isActive, Long projectId, Long formId, Long enumeratorId, Long commentId, Long deviceId){
        List<Submission> submissions = submissionRepository.findByIsActive(isActive, projectId, formId, enumeratorId, commentId, deviceId);
        return submissions;
    }

    public Map<String, Integer> getSubmissions(int length, String dateType) {
        DateEnum.validateDateEnum(dateType);
        LocalDateTime startDate = LocalDateTime.now();
        HashMap<String, Integer> submissions = new HashMap<>();

        if(DateEnum.MONTH.getValue().equals(dateType)) {
            if(length > DateEnum.MONTH.getPeriod())
                throw new BadRequestException(CustomResponseCode.BAD_REQUEST, DateEnum.lengthError() + DateEnum.MONTH.getPeriod());

            for(int i = 1; i <= length; i++) {
                submissions.put(String.valueOf(startDate.getMonth()), getSubmissionsPerMonth(startDate));
                startDate = startDate.minusMonths(1);
            }
        }

        if(DateEnum.WEEK.getValue().equals(dateType)) {
            if(length > DateEnum.WEEK.getPeriod())
                throw new BadRequestException(CustomResponseCode.BAD_REQUEST, DateEnum.lengthError() + DateEnum.WEEK.getPeriod());

            for(int i = 1; i <= length; i++) {
                submissions.put(String.valueOf(startDate.getDayOfWeek()), getSubmissionsPerDay(startDate));
                startDate = startDate.minusDays(1);
            }
        }

        if(DateEnum.DAY.getValue().equals(dateType)) {
            if(length > DateEnum.DAY.getPeriod())
                throw new BadRequestException(CustomResponseCode.BAD_REQUEST, DateEnum.lengthError() + DateEnum.DAY.getPeriod());

            for(int i = 1; i <= length; i++) {
                submissions.put(String.valueOf(startDate.getDayOfMonth()), getSubmissionsPerDay(startDate));
                startDate = startDate.minusDays(1);
            }
        }
        return submissions;
    }

    private int getSubmissionsPerMonth(LocalDateTime startDate) {
        LocalDateTime endDate = startDate.minusMonths(1);
        return submissionRepository.findSubmissionBySubmissionDateBetween(endDate, startDate).size();
    }

    private int getSubmissionsPerDay(LocalDateTime startDate) {
        LocalDateTime endDate = startDate.minusDays(1);
        return submissionRepository.findSubmissionBySubmissionDateBetween(endDate, startDate).size();
    }

    public int getSurveysForProject(List<Project> projects, SubmissionStatus status) {
        if(projects.size() == 0) return 0;
        int count = 0;
        if(Objects.isNull(status)) {
            for (Project project : projects) {
                count += submissionRepository.findSubmissionByProjectId(project.getId()).size();
            }
        }else {
            for (Project project : projects) {
                count += submissionRepository.findSubmissionByProjectIdAndStatus(project.getId(), status).size();
            }
        }
        return count;
    }

    public int getSurveysForProjectEnumerator(List<ProjectEnumerator> projectEnumerators, SubmissionStatus status) {
        if(projectEnumerators.size() == 0) return 0;
        int count = 0;
        if(Objects.isNull(status)) {
            for (ProjectEnumerator projectEnumerator : projectEnumerators) {
                count += submissionRepository.findSubmissionByProjectId(projectEnumerator.getProjectId()).size();
            }
        }else {
            for (ProjectEnumerator projectEnumerator : projectEnumerators) {
                count += submissionRepository.findSubmissionByProjectIdAndStatus(projectEnumerator.getProjectId(), status).size();
            }
        }
        return count;
    }


}
