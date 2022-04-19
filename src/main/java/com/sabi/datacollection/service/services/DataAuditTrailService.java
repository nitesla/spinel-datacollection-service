package com.sabi.datacollection.service.services;

import com.sabi.datacollection.core.enums.AuditTrailFlag;
import com.sabi.datacollection.core.models.DataAuditTrail;
import com.sabi.datacollection.service.repositories.DataAuditTrailRepository;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.utils.CustomResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@SuppressWarnings("ALL")
@Service
@Slf4j
public class DataAuditTrailService {

    private final DataAsyncService asyncService;
    private final DataAuditTrailRepository auditTrailRepository;
    private final ModelMapper mapper;


    public DataAuditTrailService(DataAsyncService asyncService, DataAuditTrailRepository auditTrailRepository, ModelMapper mapper) {
        this.asyncService = asyncService;
        this.auditTrailRepository = auditTrailRepository;
        this.mapper = mapper;
    }

    public void logEvent(String username, String event, AuditTrailFlag flag, String request,
                         int status, String ipAddress) {
        log.info(":::::: PROCESSING AUDIT TRAIL ::");
        asyncService.processAudit(username, event, flag, request, status, ipAddress);
    }



    public DataAuditTrail getAudit(Long id){
        DataAuditTrail auditTrail  = auditTrailRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested id does not exist!"));
        return auditTrail;
    }



    public Page<DataAuditTrail> findAll(String username, String event, String flag, LocalDateTime startDate, LocalDateTime endDate, PageRequest pageRequest ){
        Page<DataAuditTrail> auditTrails = auditTrailRepository.audits(username,event,flag,startDate,endDate,pageRequest);
        if(auditTrails == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }
        return auditTrails;

    }
}
