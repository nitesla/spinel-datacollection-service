package com.spinel.datacollection.service.services;


import com.spinel.datacollection.core.dto.request.CommentDictionaryDto;
import com.spinel.datacollection.core.dto.request.EnableDisableDto;
import com.spinel.datacollection.core.dto.response.CommentDictionaryResponseDto;
import com.spinel.datacollection.core.models.CommentDictionary;
import com.spinel.datacollection.service.helper.Validations;
import com.spinel.datacollection.service.repositories.CommentDictionaryRepository;
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
public class CommentDictionaryService {

    private final CommentDictionaryRepository commentDictionaryRepository;
    private final ModelMapper mapper;
    private final Validations validations;

    public CommentDictionaryService(CommentDictionaryRepository commentDictionaryRepository, ModelMapper mapper, Validations validations) {
        this.commentDictionaryRepository = commentDictionaryRepository;
        this.mapper = mapper;
        this.validations = validations;
    }

    public CommentDictionaryResponseDto createCommentDictionary (CommentDictionaryDto request) {
        System.err.println(request);
        validations.validateCommentDictionary(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        CommentDictionary commentDictionary = mapper.map(request, CommentDictionary.class);
        CommentDictionary commentDictionaryExist = commentDictionaryRepository.findByName(request.getName());
        if(commentDictionaryExist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "Comment Dictionary already exist");
        }
        commentDictionary.setCreatedBy(userCurrent.getId());
        commentDictionary.setIsActive(true);
        commentDictionaryRepository.save(commentDictionary);
        log.info("Created new Comment Dictionary - {}", commentDictionary);
        return mapper.map(commentDictionary, CommentDictionaryResponseDto.class);
    }

    public CommentDictionaryResponseDto updateCommentDictionary(CommentDictionaryDto request) {
        validations.validateCommentDictionary(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        CommentDictionary commentDictionary = commentDictionaryRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Comment Dictionary Id does not exist!"));
        mapper.map(request, commentDictionary);
        commentDictionary.setUpdatedBy(userCurrent.getId());
        commentDictionaryRepository.save(commentDictionary);
        log.info("Comment Dictionary record updated - {}", commentDictionary);
        return mapper.map(commentDictionary, CommentDictionaryResponseDto.class);
    }

    public CommentDictionaryResponseDto findCommentDictionaryById(Long id){
        CommentDictionary commentDictionary = commentDictionaryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Comment Dictionary Id does not exist!"));
        return mapper.map(commentDictionary, CommentDictionaryResponseDto.class);
    }


    public Page<CommentDictionary> findAll(String name, PageRequest pageRequest ) {
        Page<CommentDictionary> commentDictionaries = commentDictionaryRepository.findCommentDictionaries(name, pageRequest);
        if (commentDictionaries == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, "No record found !");
        }
        return commentDictionaries;

    }

    public void enableDisableState (EnableDisableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        CommentDictionary commentDictionary = commentDictionaryRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Comment Dictionary Id does not exist!"));
        commentDictionary.setIsActive(request.getIsActive());
        commentDictionary.setUpdatedBy(userCurrent.getId());
        commentDictionaryRepository.save(commentDictionary);

    }

    public List<CommentDictionary> getAll(Boolean isActive){
        return commentDictionaryRepository.findByIsActive(isActive);
    }

}
