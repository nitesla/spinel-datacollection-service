package com.sabi.datacollection.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sabi.datacollection.core.dto.request.EnableDisableDto;
import com.sabi.datacollection.core.dto.request.JobRequestDto;
import com.sabi.datacollection.core.dto.response.JobRequestResponseDto;
import com.sabi.datacollection.core.enums.GeneralStatus;
import com.sabi.datacollection.core.models.JobRequest;
import com.sabi.datacollection.service.helper.Validations;
import com.sabi.datacollection.service.repositories.JobRequestRepository;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class JobRequestService {
    private final JobRequestRepository jobRequestRepository;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;
    private final Validations validations;



    /** <summary>
     * jobRequest creation
     * </summary>
     * <remarks>this method is responsible for creation of new jobRequests</remarks>
     */

    public JobRequestResponseDto createJobRequest(JobRequestDto request) {
        validations.validateJobRequest(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        JobRequest jobRequest = mapper.map(request, JobRequest.class);
        JobRequest jobRequestExist = jobRequestRepository.findByProjectId(request.getProjectId());
        if(jobRequestExist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " jobRequest already exist");
        }
        jobRequest.setCreatedBy(userCurrent.getId());
        jobRequest.setIsActive(true);
        jobRequest.setRequestedDate(LocalDateTime.now());
        jobRequest.setStatus(GeneralStatus.PENDING);
        jobRequest = jobRequestRepository.save(jobRequest);
        log.debug("Create new jobRequest - {}"+ new Gson().toJson(jobRequest));
        return mapper.map(jobRequest, JobRequestResponseDto.class);
    }


    /** <summary>
     * jobRequest update
     * </summary>
     * <remarks>this method is responsible for updating already existing jobRequests</remarks>
     */

    public JobRequestResponseDto updateJobRequest(JobRequestDto request) {
        validations.validateJobRequest(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        JobRequest jobRequest = jobRequestRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested jobRequest Id does not exist!"));
        mapper.map(request, jobRequest);
        jobRequest.setUpdatedBy(userCurrent.getId());
        jobRequestRepository.save(jobRequest);
        log.debug("jobRequest record updated - {}"+ new Gson().toJson(jobRequest));
        return mapper.map(jobRequest, JobRequestResponseDto.class);
    }


    /** <summary>
     * Find jobRequest
     * </summary>
     * <remarks>this method is responsible for getting a single record</remarks>
     */
    public JobRequestResponseDto findJobRequest(Long id){
        JobRequest jobRequest = jobRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested jobRequest Id does not exist!"));

        return mapper.map(jobRequest, JobRequestResponseDto.class);
    }


    /** <summary>
     * Find all jobRequest
     * </summary>
     * <remarks>this method is responsible for getting all records in pagination</remarks>
     */
    public Page<JobRequest> findAll(Long userId, Long projectId,
                                    String status,
//                                    LocalDateTime requestedDate,
//                                    LocalDateTime responseDate,
                                    PageRequest pageRequest ){
        Page<JobRequest> jobRequest = jobRequestRepository.findJobRequests(userId, projectId, status, pageRequest);
        if(jobRequest == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }

        return jobRequest;
    }


    /** <summary>
     * Enable disenable
     * </summary>
     * <remarks>this method is responsible for enabling and disabling a jobRequest</remarks>
     */
    public void enableDisEnableJobRequest (EnableDisableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        JobRequest jobRequest = jobRequestRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested jobRequest Id does not exist!"));
        jobRequest.setIsActive(request.getIsActive());
        jobRequest.setUpdatedBy(userCurrent.getId());
        jobRequestRepository.save(jobRequest);

    }

    public List<JobRequest> getAll(Boolean isActive){
        return jobRequestRepository.findByIsActive(isActive);

    }
}
