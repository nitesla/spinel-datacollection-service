package com.spinel.datacollection.service.services;



import com.spinel.datacollection.core.dto.request.EnableDisableDto;
import com.spinel.datacollection.core.dto.request.SubmissionCommentDto;
import com.spinel.datacollection.core.dto.response.SubmissionCommentResponseDto;
import com.spinel.datacollection.core.models.SubmissionComment;
import com.spinel.datacollection.service.helper.Validations;
import com.spinel.datacollection.service.repositories.SubmissionCommentRepository;
import com.spinel.framework.exceptions.ConflictException;
import com.spinel.framework.exceptions.NotFoundException;
import com.spinel.framework.models.User;
import com.spinel.framework.service.TokenService;
import com.spinel.framework.utils.CustomResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@SuppressWarnings("ALL")
@Slf4j
@Service
public class SubmissionCommentService {


    private final SubmissionCommentRepository submissionCommentRepository;
    private final ModelMapper mapper;
    private final Validations validations;

    public SubmissionCommentService(SubmissionCommentRepository submissionCommentRepository, ModelMapper mapper, Validations validations) {
        this.submissionCommentRepository = submissionCommentRepository;
        this.mapper = mapper;
        this.validations = validations;
    }

    public SubmissionCommentResponseDto createSubmissionComment(SubmissionCommentDto request) {
        validations.validateSubmissionComment(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        SubmissionComment submissionComment = mapper.map(request, SubmissionComment.class);
        SubmissionComment submissionCommentExist = submissionCommentRepository.findBySubmissionIdAndCommentId(request.getSubmissionId(), request.getCommentId());
        if(submissionCommentExist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Submission Comment already exist");
        }
        submissionComment.setCreatedBy(userCurrent.getId());
        submissionComment.setIsActive(true);
        submissionCommentRepository.save(submissionComment);
        log.info("Created new Submission Comment - {}", submissionComment);
        return mapper.map(submissionComment, SubmissionCommentResponseDto.class);
    }

    public SubmissionCommentResponseDto updateSubmissionComment(SubmissionCommentDto request) {
        validations.validateSubmissionComment(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        SubmissionComment submissionComment = submissionCommentRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Submission Comment Id does not exist!"));
        mapper.map(request, submissionComment);
        submissionComment.setUpdatedBy(userCurrent.getId());
        submissionCommentRepository.save(submissionComment);
        log.info("Submission Comment record updated - {}", submissionComment);
        return mapper.map(submissionComment, SubmissionCommentResponseDto.class);
    }

    public SubmissionCommentResponseDto findSubmissionCommentById(Long id){
        SubmissionComment submissionComment = submissionCommentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Submission Comment Id does not exist!"));
        return mapper.map(submissionComment, SubmissionCommentResponseDto.class);
    }


    public Page<SubmissionComment> findAll(Long submissionId, Long commentId, String additionalInfo, PageRequest pageRequest ) {
        Page<SubmissionComment> projectCategories = submissionCommentRepository.findSubmissionComment(submissionId, commentId, additionalInfo, pageRequest);
        if (projectCategories == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return projectCategories;

    }

    public void enableDisableState (EnableDisableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        SubmissionComment submissionComment = submissionCommentRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested SubmissionComment Id does not exist!"));
        submissionComment.setIsActive(request.getIsActive());
        submissionComment.setUpdatedBy(userCurrent.getId());
        submissionCommentRepository.save(submissionComment);

    }

    public List<SubmissionComment> getAll(Boolean isActive){
        return submissionCommentRepository.findByIsActive(isActive);
    }
}
