package com.sabi.datacollection.service.services;

import com.sabi.datacollection.core.dto.response.BackOfficeResponseDto;
import com.sabi.datacollection.service.repositories.EnumeratorRepository;
import com.sabi.datacollection.service.repositories.SubmissionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BackOfficeDashboardService {

    @Autowired
    private EnumeratorRepository enumeratorRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    public BackOfficeResponseDto populateBackOfficeInfo(){
        BackOfficeResponseDto responseDto = new BackOfficeResponseDto();
        responseDto.setActiveEnumerator(enumeratorRepository.countAllByIsActive(true));
        responseDto.setTotalSubmission(submissionRepository.findAll().size());
//        responseDto.setPendingPayout();
        return responseDto;
    }

}
