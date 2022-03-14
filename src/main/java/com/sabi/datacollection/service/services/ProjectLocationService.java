package com.sabi.datacollection.service.services;

import com.sabi.datacollection.core.dto.request.EnableDisableDto;
import com.sabi.datacollection.core.dto.request.ProjectLocationDto;
import com.sabi.datacollection.core.dto.response.ProjectLocationResponseDto;
import com.sabi.datacollection.core.models.Project;
import com.sabi.datacollection.core.models.ProjectLocation;
import com.sabi.datacollection.service.helper.Validations;
import com.sabi.datacollection.service.repositories.ProjectLocationRepository;
import com.sabi.framework.exceptions.ConflictException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.models.User;
import com.sabi.framework.service.TokenService;
import com.sabi.framework.utils.CustomResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@SuppressWarnings("ALL")
@Slf4j
@Service
public class ProjectLocationService {

    private final ProjectLocationRepository projectLocationRepository;
    private final ModelMapper mapper;
    private final Validations validations;


    public ProjectLocationService(ProjectLocationRepository projectLocationRepository, ModelMapper mapper, Validations validations) {
        this.projectLocationRepository = projectLocationRepository;
        this.mapper = mapper;
        this.validations = validations;
    }

    public ProjectLocationResponseDto createProjectLocation(ProjectLocationDto request) {
        validations.validateProjectLocation(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        ProjectLocation projectLocation = mapper.map(request, ProjectLocation.class);
        ProjectLocation projectLocationExist = projectLocationRepository.findProjectLocationByName(request.getName());
        if (projectLocationExist != null ) {
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "Project Location already exists");
        }
        projectLocation.setCreatedBy(userCurrent.getId());
        projectLocation.setIsActive(true);
        projectLocationRepository.save(projectLocation);
        log.info("Created new Project Location - {}", projectLocation);
        return mapper.map(projectLocation, ProjectLocationResponseDto.class);
    }

    public ProjectLocationResponseDto updateProjectLocation(ProjectLocationDto request) {
        validations.validateProjectLocation(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        ProjectLocation projectLocation = projectLocationRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Enter valid Project Location Id"));
        mapper.map(request, projectLocation);
        projectLocation.setCreatedBy(userCurrent.getId());
        projectLocationRepository.save(projectLocation);
        log.info("Updated Project Location - {}", projectLocation);
        return mapper.map(projectLocation, ProjectLocationResponseDto.class);
    }

    public ProjectLocationResponseDto findProjectLocationById(Long id) {
        ProjectLocation projectLocation = projectLocationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Enter valid Project Location Id"));
        return mapper.map(projectLocation, ProjectLocationResponseDto.class);
    }

    public Page<ProjectLocation> findProjectLocationByLocationId(Long locationId, Pageable pageable) {
        Page<ProjectLocation> projectLocationPage = projectLocationRepository.findProjectLocationByLocationId(locationId, pageable);
        if (projectLocationPage == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, "No record found");
        }
        return projectLocationPage;
    }

    public Page<ProjectLocation> findProjectLocationByLocationType(String location, Pageable pageable) {
        Page<ProjectLocation> projectLocationPage = projectLocationRepository.findProjectLocationByLocationType(location, pageable);
        if (projectLocationPage == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, "No record found");
        }
        return projectLocationPage;
    }

    public Page<ProjectLocation> findAll(String name, String locationType, Pageable pageable) {
        Page<ProjectLocation> projectLocationPage = projectLocationRepository
                .findProjectLocations(name, locationType, pageable);
        if (projectLocationPage == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, "No record found");
        }
        return projectLocationPage;
    }

    public void enableDisableState (EnableDisableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        ProjectLocation projectLocation = projectLocationRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Project Location Id does not exist!"));
        projectLocation.setIsActive(request.getIsActive());
        projectLocation.setUpdatedBy(userCurrent.getId());
        projectLocationRepository.save(projectLocation);

    }

    public List<ProjectLocation> getAll(Boolean isActive) {
        return projectLocationRepository.findProjectLocationByIsActive(isActive);
    }

}
