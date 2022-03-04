package com.sabi.datacollection.service.services;

import com.sabi.datacollection.core.dto.request.DataSetDto;
import com.sabi.datacollection.core.dto.request.EnableDisableDto;
import com.sabi.datacollection.core.dto.response.DataSetResponseDto;
import com.sabi.datacollection.core.models.DataSet;
import com.sabi.datacollection.service.helper.Validations;
import com.sabi.datacollection.service.repositories.DataSetRepository;
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
public class DataSetService {

    private final DataSetRepository dataSetRepository;
    private final ModelMapper mapper;
    private final Validations validations;

    public DataSetService(DataSetRepository dataSetRepository, ModelMapper mapper, Validations validations) {
        this.dataSetRepository = dataSetRepository;
        this.mapper = mapper;
        this.validations = validations;
    }

    public DataSetResponseDto createDataSet(DataSetDto request) {
        validations.validateDataSet(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        DataSet dataSet = mapper.map(request, DataSet.class);
        DataSet dataSetExist = dataSetRepository.findByName(request.getName());
        if(dataSetExist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, "DataSet already exist");
        }
        dataSet.setCreatedBy(userCurrent.getId());
        dataSet.setIsActive(true);
        dataSetRepository.save(dataSet);
        log.info("Created new DataSet Category - {}", dataSet);
        return mapper.map(dataSet, DataSetResponseDto.class);
    }

    public DataSetResponseDto updateDataSet(DataSetDto request) {
        validations.validateDataSet(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        DataSet dataSet = dataSetRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Dataset Id does not exist!"));
        mapper.map(request, dataSet);
        dataSet.setUpdatedBy(userCurrent.getId());
        dataSetRepository.save(dataSet);
        log.info("Dataset record updated - {}", dataSet);
        return mapper.map(dataSet, DataSetResponseDto.class);
    }

    public DataSetResponseDto findDataSetById(Long id){
        DataSet dataSet = dataSetRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Dataset Id does not exist!"));
        return mapper.map(dataSet, DataSetResponseDto.class);
    }


    public Page<DataSet> findAll(String name, PageRequest pageRequest ) {
        Page<DataSet> dataSets = dataSetRepository.findDataSets(name, pageRequest);
        if (dataSets == null) {
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, "No record found !");
        }
        return dataSets;

    }

    public void enableDisableState (EnableDisableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        DataSet dataSet = dataSetRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested DataSet Id does not exist!"));
        dataSet.setIsActive(request.getIsActive());
        dataSet.setUpdatedBy(userCurrent.getId());
        dataSetRepository.save(dataSet);

    }

    public List<DataSet> getAll(Boolean isActive){
        return dataSetRepository.findByIsActive(isActive);
    }
}
