package com.spinel.datacollection.service.services;


import com.spinel.datacollection.core.dto.request.EnableDisableDto;
import com.spinel.datacollection.core.dto.request.SectorDto;
import com.spinel.datacollection.core.dto.response.SectorResponseDto;
import com.spinel.datacollection.core.models.Sector;
import com.spinel.datacollection.service.helper.Validations;
import com.spinel.datacollection.service.repositories.SectorRepository;

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
public class SectorService {

    private final SectorRepository sectorRepository;
    private final ModelMapper mapper;
    private final Validations validations;

    public SectorService(SectorRepository sectorRepository, ModelMapper mapper, Validations validations) {
        this.sectorRepository = sectorRepository;
        this.mapper = mapper;
        this.validations = validations;
    }

    public SectorResponseDto createSector(SectorDto request) {
        validations.validateSector(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Sector sector = mapper.map(request, Sector.class);
        Sector sectorExist = sectorRepository.findByName(request.getName());
        if(sectorExist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Sector already exist");
        }
        sector.setCreatedBy(userCurrent.getId());
        sector.setIsActive(true);
        sectorRepository.save(sector);
        log.info("Created new Sector - {}", sector);
        return mapper.map(sector, SectorResponseDto.class);
    }

    public SectorResponseDto updateSector(SectorDto request) {
        validations.validateSector(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Sector sector = sectorRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Sector Id does not exist!"));
        mapper.map(request, sector);
        sector.setUpdatedBy(userCurrent.getId());
        sectorRepository.save(sector);
        log.info("Sector record updated - {}", sector);
        return mapper.map(sector, SectorResponseDto.class);
    }

    public SectorResponseDto findSectorById(Long id){
        Sector sector = sectorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Sector Id does not exist!"));
        return mapper.map(sector, SectorResponseDto.class);
    }


    public Page<Sector> findAll(String name, PageRequest pageRequest ) {
        Page<Sector> sectors = sectorRepository.findSectors(name, pageRequest);
        if (sectors == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return sectors;

    }

    public void enableDisableState (EnableDisableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Sector sector = sectorRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Sector Id does not exist!"));
        sector.setIsActive(request.getIsActive());
        sector.setUpdatedBy(userCurrent.getId());
        sectorRepository.save(sector);

    }

    public List<Sector> getAll(Boolean isActive){
        return sectorRepository.findByIsActive(isActive);
    }
}
