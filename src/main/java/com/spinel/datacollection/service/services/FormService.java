package com.spinel.datacollection.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.spinel.datacollection.core.dto.request.EnableDisableDto;
import com.spinel.datacollection.core.dto.request.FormDto;
import com.spinel.datacollection.core.dto.request.GetRequestDto;
import com.spinel.datacollection.core.dto.response.FormResponseDto;
import com.spinel.datacollection.core.models.Form;
import com.spinel.datacollection.service.helper.GenericSpecification;
import com.spinel.datacollection.service.helper.SearchCriteria;
import com.spinel.datacollection.service.helper.SearchOperation;
import com.spinel.datacollection.service.helper.Validations;
import com.spinel.datacollection.service.repositories.FormRepository;
import com.spinel.framework.exceptions.BadRequestException;
import com.spinel.framework.exceptions.ConflictException;
import com.spinel.framework.exceptions.NotFoundException;
import com.spinel.framework.models.User;
import com.spinel.framework.service.TokenService;
import com.spinel.framework.utils.CustomResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


/**
 *
 * This class is responsible for all business logic for form
 */


@Slf4j
@Service
public class FormService {

    @Autowired
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

    public Page<Form> findFilteredPage(GetRequestDto request) {
        GenericSpecification<Form> genericSpecification = new GenericSpecification<Form>();

            if (request.getFilterCriteria() != null) {
                request.getFilterCriteria().forEach(filter -> {
                    if (filter.getFilterParameter() != null || filter.getFilterValue() != null) {
                        if (filter.getFilterParameter().equalsIgnoreCase("name")) {
                            genericSpecification.add(new SearchCriteria("name", filter.getFilterValue(), SearchOperation.MATCH));
                        }
                        if (filter.getFilterParameter().equalsIgnoreCase("version")) {
                            genericSpecification.add(new SearchCriteria("version", filter.getFilterValue(), SearchOperation.MATCH));
                        }
                        if (filter.getFilterParameter().equalsIgnoreCase("description")) {
                            genericSpecification.add(new SearchCriteria("description", filter.getFilterValue(), SearchOperation.MATCH));
                        }
                        if (filter.getFilterParameter().equalsIgnoreCase("isActive")) {
                            genericSpecification.add(new SearchCriteria("isActive", Boolean.valueOf(filter.getFilterValue()), SearchOperation.EQUAL));
                        }
                        if (filter.getFilterParameter().equalsIgnoreCase("userId")) {
                            genericSpecification.add(new SearchCriteria("userId", filter.getFilterValue(), SearchOperation.EQUAL));
                        }
                        if (filter.getFilterParameter().equalsIgnoreCase("projectId")) {
                            genericSpecification.add(new SearchCriteria("projectId", filter.getFilterValue(), SearchOperation.EQUAL));
                        }
                        if (filter.getFilterParameter().equalsIgnoreCase("id")) {
                            genericSpecification.add(new SearchCriteria("id", Long.parseLong(filter.getFilterValue()), SearchOperation.EQUAL));
                        }
                    }
                });
            }


            if (request.getFilterDate() != null) {

                request.getFilterDate().forEach(filter -> {
                    if (filter.getDateParameter() != null && filter.getDateParameter().equalsIgnoreCase("createdDate")) {
                        if (filter.getStartDate() != null) {
                            if (filter.getEndDate() != null && filter.getStartDate().isAfter(filter.getEndDate()))
                                throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "startDate can't be greater than endDate");
                            LocalDate startDate = LocalDate.parse(filter.getStartDate().toString());
                            genericSpecification.add(new SearchCriteria("createdDate", startDate, SearchOperation.GREATER_THAN_EQUAL));

                        }

                        if (filter.getEndDate() != null) {
                            if (filter.getStartDate() == null)
                                throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "'startDate' must be included along with 'endDate' in the request");
                            LocalDate endDate = LocalDate.parse(filter.getEndDate().toString());
                            genericSpecification.add(new SearchCriteria("createdDate", endDate, SearchOperation.LESS_THAN_EQUAL));

                        }
                    }
                });

            }

        if (request.getSortParameter() == null || request.getSortParameter().isEmpty()) {
            request.setSortDirection("desc");
            request.setSortParameter("id");
        }

        Sort sortType = (request.getSortDirection() != null && request.getSortDirection().equalsIgnoreCase("asc"))
                ?  Sort.by(Sort.Order.asc(request.getSortParameter())) :   Sort.by(Sort.Order.desc(request.getSortParameter()));

        PageRequest pageRequest = PageRequest.of(request.getPage(), request.getPageSize(), sortType);

        return formRepository.findAll(genericSpecification, pageRequest);


    }

    public List<Form> findFilteredList(GetRequestDto request) {
        GenericSpecification<Form> genericSpecification = new GenericSpecification<Form>();
            if (request.getFilterCriteria() != null ){
                request.getFilterCriteria().forEach(filter -> {
                    if (filter.getFilterParameter() != null || filter.getFilterValue() != null) {
                        if (filter.getFilterParameter().equalsIgnoreCase("name")) {
                            genericSpecification.add(new SearchCriteria("name", filter.getFilterValue(), SearchOperation.MATCH));
                        }
                        if (filter.getFilterParameter().equalsIgnoreCase("version")) {
                            genericSpecification.add(new SearchCriteria("version", filter.getFilterValue(), SearchOperation.MATCH));
                        }
                        if (filter.getFilterParameter().equalsIgnoreCase("description")) {
                            genericSpecification.add(new SearchCriteria("description", filter.getFilterValue(), SearchOperation.MATCH));
                        }
                        if (filter.getFilterParameter().equalsIgnoreCase("isActive")) {
                            genericSpecification.add(new SearchCriteria("isActive", Boolean.valueOf(filter.getFilterValue()), SearchOperation.EQUAL));
                        }
                        if (filter.getFilterParameter().equalsIgnoreCase("userId")) {
                            genericSpecification.add(new SearchCriteria("userId", Long.parseLong(filter.getFilterValue()), SearchOperation.EQUAL));
                        }
                        if (filter.getFilterParameter().equalsIgnoreCase("projectId")) {
                            genericSpecification.add(new SearchCriteria("projectId", Long.parseLong(filter.getFilterValue()), SearchOperation.EQUAL));
                        }
                        if (filter.getFilterParameter().equalsIgnoreCase("id")) {
                            genericSpecification.add(new SearchCriteria("id", Long.parseLong(filter.getFilterValue()), SearchOperation.EQUAL));
                        }
                    }
                });
            }

            if (request.getFilterDate() != null) {
                request.getFilterDate().forEach(filter -> {
                    if (filter.getDateParameter() != null && filter.getDateParameter().equalsIgnoreCase("createdDate")) {
                        if (filter.getStartDate() != null) {
                            if (filter.getEndDate() != null && filter.getStartDate().isAfter(filter.getEndDate()))
                                throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "startDate can't be greater than endDate");
                            LocalDate startDate = LocalDate.parse(filter.getStartDate().toString());
                            genericSpecification.add(new SearchCriteria("createdDate", startDate, SearchOperation.GREATER_THAN_EQUAL));

                        }

                        if (filter.getEndDate() != null) {
                            if (filter.getStartDate() == null)
                                throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "'startDate' must be included along with 'endDate' in the request");
                            LocalDate endDate = LocalDate.parse(filter.getEndDate().toString());
                            genericSpecification.add(new SearchCriteria("createdDate", endDate, SearchOperation.LESS_THAN_EQUAL));

                        }
                    }
                });
            }

        if (request.getSortParameter() == null || request.getSortParameter().isEmpty()) {
            request.setSortDirection("desc");
            request.setSortParameter("id");
        }
        Sort sortType = (request.getSortDirection() != null && request.getSortDirection().equalsIgnoreCase("asc"))
                ? Sort.by(Sort.Order.asc(request.getSortParameter())) : Sort.by(Sort.Order.desc(request.getSortParameter()));

        return formRepository.findAll(genericSpecification, sortType);

    }


    public Page<Form> findUnfilteredPage(GetRequestDto request) {
        if (request.getSortParameter() == null || request.getSortParameter().isEmpty()) {
            request.setSortDirection("desc");
            request.setSortParameter("id");
        }
        Sort sortType = (request.getSortDirection() != null && request.getSortDirection().equalsIgnoreCase("asc"))
                ?  Sort.by(Sort.Order.asc(request.getSortParameter())) :   Sort.by(Sort.Order.desc(request.getSortParameter()));
        PageRequest pageRequest = PageRequest.of(request.getPage(), request.getPageSize(), sortType);
        return formRepository.findAll(pageRequest);
    }

    public List<Form> findUnfilteredList() {
        return formRepository.findAll();
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

    public List<Form> getAll(Boolean isActive, Long projectId, Long userId){
        List<Form> forms = formRepository.findByIsActive(isActive, projectId, userId);
        return forms;

    }


}
