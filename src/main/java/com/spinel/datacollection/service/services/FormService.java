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

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


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
    public Page<Form> findPaginated(GetRequestDto request) {
        GenericSpecification<Form> genericSpecification = new GenericSpecification<Form>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        SimpleDateFormat enUsFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        SimpleDateFormat localFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());


        request.getFilterCriteria().forEach(filter-> {
            if (filter.getFilterParameter() != null) {
                if (filter.getFilterParameter().equalsIgnoreCase("name")) {
                    genericSpecification.add(new SearchCriteria("name", filter.getFilterValue(), SearchOperation.MATCH));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("version")) {
                    genericSpecification.add(new SearchCriteria("version", filter.getFilterValue(), SearchOperation.MATCH));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("description")) {
                    genericSpecification.add(new SearchCriteria("description", filter.getFilterValue(), SearchOperation.MATCH));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("userId")) {
                    genericSpecification.add(new SearchCriteria("userId", filter.getFilterValue(), SearchOperation.EQUAL));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("projectId")) {
                    genericSpecification.add(new SearchCriteria("projectId", filter.getFilterValue(), SearchOperation.EQUAL));
                }
            }
        });

//        request.getFilterDate().forEach(filter-> {
//            if (filter.getDateParameter() != null) {
////                if (filter.getDateParameter().equalsIgnoreCase("createdDate")) {
////                    try {
////                        genericSpecification.add(new SearchCriteria("createdDate", localFormat.parse(filter.getStartDate()), SearchOperation.GREATER_THAN_EQUAL));
//////                        genericSpecification.add(new SearchCriteria("createdDate", localFormat.parse(String.valueOf(filter.getEndDate())), SearchOperation.LESS_THAN_EQUAL));
////                    } catch (ParseException e) {
////                        throw new RuntimeException(e);
////                    }
////                }
//            }
//        });


        Sort sortType = (request.getSortDirection() != null && request.getSortDirection().equalsIgnoreCase("asc"))
                ?  Sort.by(Sort.Order.asc(request.getSortBy())) :   Sort.by(Sort.Order.desc(request.getSortBy()));

        PageRequest pageRequest = PageRequest.of(request.getPage(), request.getPageSize(), sortType);

        return formRepository.findAll(genericSpecification, pageRequest);


    }

    public List<Form> findList(GetRequestDto request) {
        GenericSpecification<Form> genericSpecification = new GenericSpecification<Form>();

        request.getFilterCriteria().forEach(filter-> {
            if (filter.getFilterParameter() != null) {
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
                    genericSpecification.add(new SearchCriteria("isActive", filter.getFilterValue(), SearchOperation.EQUAL));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("userId")) {
                    genericSpecification.add(new SearchCriteria("userId", Long.parseLong(filter.getFilterValue()), SearchOperation.EQUAL));
                }
                if (filter.getFilterParameter().equalsIgnoreCase("projectId")) {
                    genericSpecification.add(new SearchCriteria("projectId", Long.parseLong(filter.getFilterValue()), SearchOperation.EQUAL));
                }
            }
        });

        request.getFilterDate().forEach(filter-> {
//            if (filter.getDateParameter() != null) {
//                if (filter.getDateParameter().equalsIgnoreCase("createdDate")) {
//                    genericSpecification.add(new SearchCriteria("createdDate", filter.getStartDate(), SearchOperation.GREATER_THAN_EQUAL));
//                    genericSpecification.add(new SearchCriteria("createdDate", filter.getEndDate(), SearchOperation.LESS_THAN_EQUAL));
//                }
//            }
        });


        Sort sortType = (request.getSortDirection() != null && request.getSortDirection().equalsIgnoreCase("asc"))
                ?  Sort.by(Sort.Order.asc(request.getSortBy())) :   Sort.by(Sort.Order.desc(request.getSortBy()));

        return formRepository.findAll(genericSpecification, sortType);


    }


    public Page<Form> getEntities(GetRequestDto request) {
        Sort sortType = (request.getSortDirection() != null && request.getSortDirection().equalsIgnoreCase("asc"))
                ?  Sort.by(Sort.Order.asc(request.getSortBy())) :   Sort.by(Sort.Order.desc(request.getSortBy()));
        PageRequest pageRequest = PageRequest.of(request.getPage(), request.getPageSize(), sortType);
        return formRepository.findAll(pageRequest);
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
