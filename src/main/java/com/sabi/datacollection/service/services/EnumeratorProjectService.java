package com.sabi.datacollection.service.services;



import com.sabi.datacollection.core.dto.request.EnableDisableDto;
import com.sabi.datacollection.core.dto.request.EnumeratorProjectDto;
import com.sabi.datacollection.core.dto.response.EnumeratorProjectResponseDto;
import com.sabi.datacollection.core.enums.Status;
import com.sabi.datacollection.core.models.EnumeratorProject;
import com.sabi.datacollection.service.helper.Validations;
import com.sabi.datacollection.service.repositories.EnumeratorProjectRepository;
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
import java.util.List;

@SuppressWarnings("ALL")
@Slf4j
@Service
public class EnumeratorProjectService {


    private final EnumeratorProjectRepository enumeratorRatingRepository;
    private final ModelMapper mapper;
    private final Validations validations;

    public EnumeratorProjectService(EnumeratorProjectRepository enumeratorRatingRepository, ModelMapper mapper, Validations validations) {
        this.enumeratorRatingRepository = enumeratorRatingRepository;
        this.mapper = mapper;
        this.validations = validations;
    }

    public EnumeratorProjectResponseDto createEnumeratorProject(EnumeratorProjectDto request) {
        validations.validateEnumeratorProject(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        EnumeratorProject enumeratorRating = mapper.map(request, EnumeratorProject.class);
        EnumeratorProject enumeratorRatingExist = enumeratorRatingRepository.findByEnumeratorIdAndProjectId(request.getEnumeratorId(), request.getProjectId());
        if(enumeratorRatingExist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Enumerator Rating already exist");
        }
        enumeratorRating.setCreatedBy(userCurrent.getId());
        enumeratorRating.setIsActive(true);
        enumeratorRatingRepository.save(enumeratorRating);
        log.info("Created new Enumerator Rating - {}", enumeratorRating);
        return mapper.map(enumeratorRating, EnumeratorProjectResponseDto.class);
    }

    public EnumeratorProjectResponseDto updateEnumeratorProject(EnumeratorProjectDto request) {
        validations.validateEnumeratorProject(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        EnumeratorProject enumeratorRating = enumeratorRatingRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Enumerator Rating Id does not exist!"));
        mapper.map(request, enumeratorRating);
        enumeratorRating.setUpdatedBy(userCurrent.getId());
        enumeratorRatingRepository.save(enumeratorRating);
        log.info("Enumerator Rating record updated - {}", enumeratorRating);
        return mapper.map(enumeratorRating, EnumeratorProjectResponseDto.class);
    }

    public EnumeratorProjectResponseDto findEnumeratorProjectById(Long id){
        EnumeratorProject enumeratorRating = enumeratorRatingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Enumerator Rating Id does not exist!"));
        return mapper.map(enumeratorRating, EnumeratorProjectResponseDto.class);
    }


    public Page<EnumeratorProject> findAll(Long projectId, Long enumeratorId, String verificationStatus, LocalDateTime assignedDate, LocalDateTime completionDate, Status status, PageRequest pageRequest ) {
        Page<EnumeratorProject> projectCategories = enumeratorRatingRepository.findEnumeratorProjects(projectId, enumeratorId, verificationStatus, assignedDate, completionDate, status, pageRequest);
        if (projectCategories == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return projectCategories;

    }

    public void enableDisableState (EnableDisableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        EnumeratorProject enumeratorRating = enumeratorRatingRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested EnumeratorProject Id does not exist!"));
        enumeratorRating.setIsActive(request.getIsActive());
        enumeratorRating.setUpdatedBy(userCurrent.getId());
        enumeratorRatingRepository.save(enumeratorRating);

    }

    public List<EnumeratorProject> getAll(Boolean isActive){
        return enumeratorRatingRepository.findByIsActive(isActive);
    }
}
