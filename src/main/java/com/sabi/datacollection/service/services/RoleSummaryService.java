package com.sabi.datacollection.service.services;

import com.sabi.datacollection.core.dto.response.RoleSummaryResponseDto;
import com.sabi.framework.repositories.RoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RoleSummaryService {

    @Autowired
    private RoleRepository roleRepository;

    public RoleSummaryResponseDto roleSummary(){
        RoleSummaryResponseDto responseDto = new RoleSummaryResponseDto();
        responseDto.setTotalActiveRole(roleRepository.countAllByIsActive(true));
        log.info("Active role: " + responseDto.getTotalActiveRole());
        responseDto.setTotalInactiveRole(roleRepository.countAllByIsActive(false));
        log.info("Inactive role: " + responseDto.getTotalInactiveRole());
        responseDto.setTotalRoles(roleRepository.findAll().size());
        responseDto.setNumberOfAdmin(roleRepository.countAllByName("Admin"));
        return  responseDto;
    }

}
