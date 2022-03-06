package com.sabi.datacollection.service.services;

import com.sabi.datacollection.core.dto.request.CommentDictionaryDto;
import com.sabi.datacollection.core.dto.request.EnableDisableDto;
import com.sabi.datacollection.core.dto.response.CommentDictionaryResponseDto;
import com.sabi.datacollection.core.models.CommentDictionary;
import com.sabi.datacollection.service.helper.Validations;
import com.sabi.datacollection.service.repositories.CommentDictionaryRepository;
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
