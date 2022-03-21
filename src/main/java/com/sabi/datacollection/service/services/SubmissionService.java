package com.sabi.datacollection.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sabi.datacollection.core.dto.request.EnableDisableDto;
import com.sabi.datacollection.core.dto.request.SubmissionDto;
import com.sabi.datacollection.core.dto.response.SubmissionResponseDto;
import com.sabi.datacollection.core.enums.Status;
import com.sabi.datacollection.core.models.CommentDictionary;
import com.sabi.datacollection.core.models.Submission;
import com.sabi.datacollection.service.helper.Validations;
import com.sabi.datacollection.service.repositories.CommentDictionaryRepository;
import com.sabi.datacollection.service.repositories.SubmissionRepository;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;


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
    public Page<Submission> findAll(Long projectId, Long formId, Status status, Long enumeratorId,  PageRequest pageRequest ){
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

    public List<Submission> getAll(Boolean isActive){
        List<Submission> submissions = submissionRepository.findByIsActive(isActive);
        return submissions;

    }


}
