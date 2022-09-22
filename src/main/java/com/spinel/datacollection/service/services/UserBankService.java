package com.spinel.datacollection.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.spinel.datacollection.core.dto.request.EnableDisableDto;
import com.spinel.datacollection.core.dto.request.UserBankRequestDto;
import com.spinel.datacollection.core.dto.response.UserBankResponseDto;
import com.spinel.datacollection.core.models.UserBank;
import com.spinel.datacollection.service.helper.Validations;
import com.spinel.datacollection.service.repositories.UserBankRepository;
import com.spinel.framework.exceptions.NotFoundException;
import com.spinel.framework.models.User;
import com.spinel.framework.service.TokenService;
import com.spinel.framework.utils.CustomResponseCode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class UserBankService {

    private final UserBankRepository userBankRepository;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;
    private final Validations validations;



    /** <summary>
     * userBank creation
     * </summary>
     * <remarks>this method is responsible for creation of new userBanks</remarks>
     */

    public UserBankResponseDto createUserBank(UserBankRequestDto request) {
        validations.validateuserBank(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        UserBank userBank = mapper.map(request, UserBank.class);
//        userBank userBankExist = userBankRepository.findByName(request.getName());
//        if(userBankExist !=null){
//            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " userBank already exist");
//        }
        userBank.setCreatedBy(userCurrent.getId());
        userBank.setIsActive(true);
        userBank = userBankRepository.save(userBank);
        log.debug("Create new userBank - {}"+ new Gson().toJson(userBank));
        return mapper.map(userBank, UserBankResponseDto.class);
    }


    /** <summary>
     * userBank update
     * </summary>
     * <remarks>this method is responsible for updating already existing userBanks</remarks>
     */

    public UserBankResponseDto updateUserBank(UserBankRequestDto request) {
        validations.validateuserBank(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        UserBank userBank = userBankRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested userBank Id does not exist!"));
        mapper.map(request, userBank);
        userBank.setUpdatedBy(userCurrent.getId());
        userBankRepository.save(userBank);
        log.debug("userBank record updated - {}"+ new Gson().toJson(userBank));
        return mapper.map(userBank, UserBankResponseDto.class);
    }


    /** <summary>
     * Find userBank
     * </summary>
     * <remarks>this method is responsible for getting a single record</remarks>
     */
    public UserBankResponseDto findUserBank(Long id){
        UserBank userBank = userBankRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested userBank Id does not exist!"));

        return mapper.map(userBank, UserBankResponseDto.class);
    }


    /** <summary>
     * Find all userBank
     * </summary>
     * <remarks>this method is responsible for getting all records in pagination</remarks>
     */
    public Page<UserBank> findAll(Long userId, Long bankId,String accountNumber, PageRequest pageRequest ){
        Page<UserBank> userBank = userBankRepository.findUserBanks(userId, bankId, accountNumber, pageRequest);
        if(userBank == null){
            throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
        }

        return userBank;
    }


    /** <summary>
     * Enable disenable
     * </summary>
     * <remarks>this method is responsible for enabling and disabling a userBank</remarks>
     */
    public void enableDisEnableUserBank (EnableDisableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        UserBank userBank = userBankRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested userBank Id does not exist!"));
        userBank.setIsActive(request.getIsActive());
        userBank.setUpdatedBy(userCurrent.getId());
        userBankRepository.save(userBank);

    }

    public List<UserBank> getAll(Boolean isActive){
        return userBankRepository.findByIsActive(isActive);

    }

}
