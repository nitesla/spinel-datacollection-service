package com.spinel.datacollection.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.spinel.datacollection.core.dto.request.EnableDisableDto;
import com.spinel.datacollection.core.dto.request.FormDto;
import com.spinel.datacollection.core.dto.response.FormResponseDto;
import com.spinel.datacollection.core.models.Form;
import com.spinel.datacollection.service.helper.Validations;
import com.spinel.datacollection.service.repositories.FormRepository;
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

import java.util.ArrayList;
import java.util.List;


/**
 *
 * This class is responsible for all business logic for form
 */


@Slf4j
@Service
public class FormService {


    private FormRepository formRepository;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;
    private final Validations validations;

    public FormService(FormRepository formRepository, ModelMapper mapper, ObjectMapper objectMapper, Validations validations) {
        this.formRepository = formRepository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.validations = validations;

    }

    /** <summary>
      * Form creation
      * </summary>
      * <remarks>this method is responsible for creation of new forms</remarks>
      */

    public FormResponseDto createForm(FormDto request) {
        validations.validateForm(request);
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Form form = mapper.map(request,Form.class);
        Form formExist = formRepository.findByName(request.getName());
        if(formExist !=null){
            throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Form already exist");
        }
        form.setCreatedBy(userCurrent.getId());
        form.setIsActive(true);
        form = formRepository.save(form);
        log.debug("Create new Form - {}"+ new Gson().toJson(form));
        return mapper.map(form, FormResponseDto.class);
    }

    public List<FormResponseDto> createBulkForm(List<FormDto> requestlist) {
        User currentUser = TokenService.getCurrentUserFromSecurityContext();
        requestlist.forEach(this.validations::validateForm);
        List<FormResponseDto> responseDtoList = new ArrayList<>();
        for (FormDto request : requestlist){
            Form form = mapper.map(request,Form.class);
            Form formExist = formRepository.findByName(request.getName());
            if(formExist !=null){
                throw new ConflictException(CustomResponseCode.CONFLICT_EXCEPTION, " Form already exist");
            }
            form.setCreatedBy(currentUser.getId());
            form.setIsActive(true);
            form = formRepository.save(form);
            responseDtoList.add(mapper.map(form,FormResponseDto.class));
        }
        return responseDtoList;
    }


//    /** <summary>
//     * Form update
//     * </summary>
//     * <remarks>this method is responsible for updating already existing forms</remarks>
//     */

//    public FormResponseDto updateForm(FormDto request) {
//        validations.validateForm(request);
//        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
//        Form form = formRepository.findById(request.getId())
//                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
//                        "Requested Form Id does not exist!"));
//        mapper.map(request, form);
//        form.setUpdatedBy(userCurrent.getId());
//        formRepository.save(form);
//        log.debug("Form record updated - {}"+ new Gson().toJson(form));
//        return mapper.map(form, FormResponseDto.class);
//    }


    /** <summary>
     * Find Form
     * </summary>
     * <remarks>this method is responsible for getting a single record</remarks>
     */
    public FormResponseDto findForm(Long id){
        Form form = formRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Form Id does not exist!"));
        return mapper.map(form,FormResponseDto.class);
    }


    /** <summary>
     * Find all Form
     * </summary>
     * <remarks>this method is responsible for getting all records in pagination</remarks>
     */
    public Page<Form> findAll(String name,String version, String description, PageRequest pageRequest ){
        Page<Form> form = formRepository.findForms(name,version, description,pageRequest);
            if(form == null){
                throw new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, " No record found !");
            }
            return form;
    }


    /** <summary>
     * Enable disable
     * </summary>
     * <remarks>this method is responsible for enabling and dis enabling a form</remarks>
     */
    public void enableDisEnableForm (EnableDisableDto request){
        User userCurrent = TokenService.getCurrentUserFromSecurityContext();
        Form form = formRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION,
                        "Requested Form Id does not exist!"));
        form.setIsActive(request.getIsActive());
        form.setUpdatedBy(userCurrent.getId());
        formRepository.save(form);

    }

    public List<Form> getAll(Boolean isActive, Long projectId, Long projectOwnerId){
        List<Form> forms = formRepository.findByIsActive(isActive, projectId, projectOwnerId);
        return forms;

    }


}
