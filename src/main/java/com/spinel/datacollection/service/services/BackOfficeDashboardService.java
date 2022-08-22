package com.spinel.datacollection.service.services;


import com.spinel.datacollection.core.dto.response.*;
import com.spinel.datacollection.core.enums.EnumeratorStatus;
import com.spinel.datacollection.service.repositories.EnumeratorRepository;
import com.spinel.datacollection.service.repositories.ProjectOwnerRepository;
import com.spinel.datacollection.service.repositories.ProjectRepository;
import com.spinel.datacollection.service.repositories.SubmissionRepository;
import com.spinel.framework.repositories.RoleRepository;
import com.spinel.framework.repositories.UserRepository;
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

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectOwnerRepository projectOwnerRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;


    public BackOfficeResponseDto populateBackOfficeInfo(){
        BackOfficeResponseDto responseDto = new BackOfficeResponseDto();
        responseDto.setActiveEnumerator(enumeratorRepository.countAllByIsActive(true));
        responseDto.setTotalSubmission(submissionRepository.findAll().size());
        //project summary
        responseDto.setTotalProject(projectRepository.findAll().size());
//        responseDto.setSubmissions(2);

        //TransactionSummary
//        responseDto.setTotalTransaction();
//        responseDto.setTotalSuccessful();
//        responseDto.setPending();
//        responseDto.setFailed();

        //EnumeratorsSummary
        responseDto.setTotalEnumerators(enumeratorRepository.findAll().size());
//        responseDto.setVerifiedEnumerators();
//        responseDto.setUnverifiedEnumerators();
        responseDto.setActiveEnumerator(enumeratorRepository.countAllByIsActive(true));
        //clientSummary
        responseDto.setActiveClient(projectOwnerRepository.countAllByIsActive(true));
        responseDto.setInactiveClient(projectOwnerRepository.countAllByIsActive(false));
//        responseDto.setVerifiedClient(projectOwnerRepository.countAllByIsActive());
//        responseDto.setUnverifiedClient();

        return responseDto;
    }

    public ClientDashboradResponseDto populateClientBackOfficeInfo() {
        ClientDashboradResponseDto responseDto = new ClientDashboradResponseDto();
        responseDto.setTotalProject(projectRepository.findAll().size());
        responseDto.setTotalSubmission(submissionRepository.findAll().size());
        responseDto.setTotalClient(projectOwnerRepository.findAll().size());
        responseDto.setTotalEnumerator(enumeratorRepository.findAll().size());
        return responseDto;

    }

    public ProjectDashboardResponse populateProjectBackOfficeInfo() {
        ProjectDashboardResponse responseDto = new ProjectDashboardResponse();
        responseDto.setTotalClient(projectOwnerRepository.findAll().size());
        responseDto.setTotalEnumerator(enumeratorRepository.findAll().size());
        responseDto.setTotalProjects(projectRepository.findAll().size());
        responseDto.setTotalSubmissions(submissionRepository.findAll().size());
        return responseDto;
    }

    public RoleDashboardResponseDto populateRoleBackOfficeInfo() {
        RoleDashboardResponseDto responseDto = new RoleDashboardResponseDto();
        responseDto.setNumberOfAdmin(userRepository.countAllByUserCategory("A"));
        responseDto.setTotalActiveRole(roleRepository.countAllByIsActive(true));
        responseDto.setTotalInactiveRole(roleRepository.countAllByIsActive(false));
        responseDto.setTotalRole(roleRepository.findAll().size());
        return responseDto;
    }

    public RoleDashboardResponseDto getTopCountriesByAdmin() {
        RoleDashboardResponseDto responseDto = new RoleDashboardResponseDto();
        responseDto.setNumberOfAdmin(roleRepository.countAllByName("Admin"));
        responseDto.setTotalActiveRole(roleRepository.countAllByIsActive(true));
        responseDto.setTotalInactiveRole(roleRepository.countAllByIsActive(false));
        responseDto.setTotalRole(roleRepository.findAll().size());
        return responseDto;
    }

    public BackOfficeUserResponseDto populateBackOfficeUserInfo(){
        BackOfficeUserResponseDto responseDto = new BackOfficeUserResponseDto();
        responseDto.setTotalActiveUsers(userRepository.countAllByIsActive(true));
        responseDto.setTotalInactiveUsers(userRepository.countAllByIsActive(false));
        responseDto.setTotalUsers(userRepository.findAll().size());
        return responseDto;
    }

    public BackOfficeAuditTrailResponseDto populateBackOfficeAuditTrailInfo(){
        BackOfficeAuditTrailResponseDto responseDto = new BackOfficeAuditTrailResponseDto();
        responseDto.setTotalUser(userRepository.findAll().size());
        responseDto.setTotalActiveClients(projectOwnerRepository.countAllByIsActive(true));
        responseDto.setTotalActiveEnumerator(enumeratorRepository.countAllByIsActive(true));
        responseDto.setTotalActiveAdmins(userRepository.countAllByUserCategory("Admin"));
        return responseDto;
    }

    public BackOfficeLocationResponseDto populateBackOfficeLocationInfo(){
        BackOfficeLocationResponseDto responseDto = new BackOfficeLocationResponseDto();
        responseDto.setTotalProject(projectRepository.findAll().size());
        responseDto.setTotalClient(projectOwnerRepository.findAll().size());
        responseDto.setTotalEnumerator(enumeratorRepository.findAll().size());
        responseDto.setTotalSubmission(submissionRepository.findAll().size());
        return responseDto;
    }

    //raise issue
    public BackOfficeEnumeratorResponseDto populateBackOfficeEnumeratorInfo(){
        BackOfficeEnumeratorResponseDto responseDto = new BackOfficeEnumeratorResponseDto();
        responseDto.setActiveEnumerator(enumeratorRepository.countAllByIsActive(true));
        responseDto.setPendingEnumerator(enumeratorRepository.countAllByStatus(EnumeratorStatus.PENDING));
        responseDto.setTotalEnumerator(enumeratorRepository.findAll().size());
        responseDto.setTotalSubmission(submissionRepository.findAll().size());
        return responseDto;
    }





}
