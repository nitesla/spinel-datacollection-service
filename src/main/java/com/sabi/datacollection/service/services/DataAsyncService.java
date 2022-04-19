package com.sabi.datacollection.service.services;

import com.sabi.datacollection.core.enums.AuditTrailFlag;
import com.sabi.datacollection.core.models.DataAuditTrail;
import com.sabi.datacollection.service.repositories.DataAuditTrailRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@SuppressWarnings("ALL")
@Slf4j
@Service
public class DataAsyncService {

    private final DataAuditTrailRepository dataAuditTrailRepository;

    public DataAsyncService(DataAuditTrailRepository dataAuditTrailRepository) {
        this.dataAuditTrailRepository = dataAuditTrailRepository;
    }

    @Async
    public void processAudit(String username, String event, AuditTrailFlag flag, String request,
                             int status, String ipAddress) {

        DataAuditTrail logs = new DataAuditTrail();
        logs.setEvent(event);
        logs.setUsername(username);
        logs.setFlag(flag.name());
        logs.setRequest(request);
        logs.setStatus(status);
        logs.setIpAddress(ipAddress);
        dataAuditTrailRepository.save(logs);
        log.info(":ASYNC  END OF RUNNING log AUDIT:",  request);
    }

}
