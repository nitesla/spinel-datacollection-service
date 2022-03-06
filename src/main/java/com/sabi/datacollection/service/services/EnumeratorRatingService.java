package com.sabi.datacollection.service.services;


import com.sabi.datacollection.core.dto.request.EnableDisableDto;
import com.sabi.datacollection.core.dto.request.EnumeratorRatingDto;
import com.sabi.datacollection.core.dto.response.EnumeratorRatingResponseDto;
import com.sabi.datacollection.core.models.EnumeratorRating;
import com.sabi.datacollection.service.helper.Validations;
import com.sabi.datacollection.service.repositories.EnumeratorRatingRepository;
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

import java.util.List;

@SuppressWarnings("ALL")
@Slf4j
@Service
public class EnumeratorRatingService {


    private final EnumeratorRatingRepository enumeratorRatingRepository;
    private final ModelMapper mapper;
    private final Validations validations;

    public EnumeratorRatingService(EnumeratorRatingRepository enumeratorRatingRepository, ModelMapper mapper, Validations validations) {
        this.enumeratorRatingRepository = enumeratorRatingRepository;
        this.mapper = mapper;
        this.validations = validations;
    }

    public EnumeratorRatingResponseDto createEnumeratorRating(EnumeratorRatingDto request) {
        validations.validateEnumeratorRating(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        EnumeratorRating enumeratorRating = mapper.map(request, EnumeratorRating.class);
        EnumeratorRating enumeratorRatingExist = enumeratorRatingRepository.findByEnumeratorProjectId(request.getEnumeratorProjectId());
        if(enumeratorRatingExist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Enumerator Rating already exist");
        }
        enumeratorRating.setCreatedBy(userCurrent.getId());
        enumeratorRating.setIsActive(true);
        enumeratorRatingRepository.save(enumeratorRating);
        log.info("Created new Enumerator Rating - {}", enumeratorRating);
        return mapper.map(enumeratorRating, EnumeratorRatingResponseDto.class);
    }

    public EnumeratorRatingResponseDto updateEnumeratorRating(EnumeratorRatingDto request) {
        validations.validateEnumeratorRating(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        EnumeratorRating enumeratorRating = enumeratorRatingRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Enumerator Rating Id does not exist!"));
        mapper.map(request, enumeratorRating);
        enumeratorRating.setUpdatedBy(userCurrent.getId());
        enumeratorRatingRepository.save(enumeratorRating);
        log.info("Enumerator Rating record updated - {}", enumeratorRating);
        return mapper.map(enumeratorRating, EnumeratorRatingResponseDto.class);
    }

    public EnumeratorRatingResponseDto findEnumeratorRatingById(Long id){
        EnumeratorRating enumeratorRating = enumeratorRatingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Enumerator Rating Id does not exist!"));
        return mapper.map(enumeratorRating, EnumeratorRatingResponseDto.class);
    }


    public Page<EnumeratorRating> findAll(Long enumeratorProjectId, Integer rating, PageRequest pageRequest ) {
        Page<EnumeratorRating> projectCategories = enumeratorRatingRepository.findEnumeratorRatings(enumeratorProjectId, rating, pageRequest);
        if (projectCategories == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return projectCategories;

    }

    public void enableDisableState (EnableDisableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        EnumeratorRating enumeratorRating = enumeratorRatingRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested EnumeratorRating Id does not exist!"));
        enumeratorRating.setIsActive(request.getIsActive());
        enumeratorRating.setUpdatedBy(userCurrent.getId());
        enumeratorRatingRepository.save(enumeratorRating);

    }

    public List<EnumeratorRating> getAll(Boolean isActive){
        return enumeratorRatingRepository.findByIsActive(isActive);
    }
}
