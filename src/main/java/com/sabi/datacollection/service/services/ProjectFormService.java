package com.sabi.datacollection.service.services;

import com.sabi.datacollection.core.dto.request.ProjectFormRequestDto;
import com.sabi.datacollection.core.dto.response.ProjectFormResponseDto;
import com.sabi.datacollection.service.helper.Validations;
import com.sabi.datacollection.service.repositories.ProjectFormRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ProjectFormService {

    private final ProjectFormRepository projectFormRepository;

    private final Validations validations;

    private final ModelMapper modelMapper;

    public ProjectFormService(ProjectFormRepository projectFormRepository, Validations validations, ModelMapper modelMapper) {
        this.projectFormRepository = projectFormRepository;
        this.validations = validations;
        this.modelMapper = modelMapper;
    }

    public ProjectFormResponseDto createProjectForm(ProjectFormRequestDto projectFormRequestDto){
        validations.validateProjectForm(projectFormRequestDto);
        //stops for now to pull the latest changes
        return null;
    }


}
