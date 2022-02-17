package com.sabi.datacollection.service.helper;


import com.sabi.datacollection.core.dto.request.BankDto;
import com.sabi.datacollection.core.dto.request.CountryDto;
import com.sabi.datacollection.core.dto.request.LGADto;
import com.sabi.datacollection.core.dto.request.StateDto;
import com.sabi.datacollection.core.models.Country;
import com.sabi.datacollection.core.models.State;
import com.sabi.datacollection.service.repositories.CountryRepository;
import com.sabi.datacollection.service.repositories.LGARepository;
import com.sabi.framework.exceptions.BadRequestException;
import com.sabi.framework.exceptions.NotFoundException;
import com.sabi.framework.repositories.RoleRepository;
import com.sabi.framework.repositories.UserRepository;
import com.sabi.framework.utils.CustomResponseCode;
import com.sabi.datacollection.service.repositories.BankRepository;
import com.sabi.datacollection.service.repositories.StateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Base64;

@SuppressWarnings("All")
@Slf4j
@Service
public class Validations {

    private RoleRepository roleRepository;
    private CountryRepository countryRepository;
    private StateRepository stateRepository;
    private LGARepository lgaRepository;
    private UserRepository userRepository;

    @Autowired
    private BankRepository bankRepository;

    public Validations(RoleRepository roleRepository, CountryRepository countryRepository, StateRepository stateRepository, LGARepository lgaRepository, UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.countryRepository = countryRepository;
        this.stateRepository = stateRepository;
        this.lgaRepository = lgaRepository;
        this.userRepository = userRepository;
    }

    public void validateState(StateDto stateDto) {
        if (stateDto.getName() == null || stateDto.getName().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");
        String valName = stateDto.getName();
        char valCharName = valName.charAt(0);
        if (Character.isDigit(valCharName)){
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name can not start with a number");
        }
        Country country = countryRepository.findById(stateDto.getCountryId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid Country id!"));
    }

    public void validateLGA (LGADto lgaDto){
        if (lgaDto.getName() == null || lgaDto.getName().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");

        String valName = lgaDto.getName();
        char valCharName = valName.charAt(0);
        if (Character.isDigit(valCharName)){
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name can not start with a number");
        }

        State state = stateRepository.findById(lgaDto.getStateId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        " Enter a valid State id!"));
    }

    public void validateCountry(CountryDto countryDto) {
        if (countryDto.getName() == null || countryDto.getName().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");
        if(countryDto.getCode() == null || countryDto.getCode().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Code cannot be empty");
    }

    public String generateReferenceNumber(int numOfDigits) {
        if (numOfDigits < 1) {
            throw new IllegalArgumentException(numOfDigits + ": Number must be equal or greater than 1");
        }
        long random = (long) Math.floor(Math.random() * 9 * (long) Math.pow(10, numOfDigits - 1)) + (long) Math.pow(10, numOfDigits - 1);
        return Long.toString(random);
    }

    public String generateCode(String code) {
        String encodedString = Base64.getEncoder().encodeToString(code.getBytes());
        return encodedString;
    }

    public void validateBank(BankDto bankDto) {
        String valName = bankDto.getName();
        char valCharName = valName.charAt(0);
        if (Character.isDigit(valCharName)){
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name can not start with a number");
        }
        if (bankDto.getName() == null || bankDto.getName().trim().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Name cannot be empty");
        if (bankDto.getCode() == null || bankDto.getCode().isEmpty())
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Bank code cannot be empty");
    }

}


