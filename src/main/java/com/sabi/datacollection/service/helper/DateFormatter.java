package com.sabi.datacollection.service.helper;

import com.sabi.framework.exceptions.BadRequestException;
import com.sabi.framework.utils.CustomResponseCode;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Slf4j
public class DateFormatter {

    public static void checkStartAndEndDate(String startDate, String endDate) {
        if((startDate != null && endDate == null) || (endDate != null && startDate == null)){
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "Date cannot be empty");
        }
        if(startDate != null) {
            if (tryParseDate(startDate).after(tryParseDate(endDate))) {
                throw new BadRequestException(CustomResponseCode.BAD_REQUEST, "start date cannot be later than end date");
            }
        }
    }

    public static Date tryParseDate(String date) {
        Date response = null;
        try {
            response = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(date);
        }catch (Exception e){
            throw new BadRequestException(CustomResponseCode.BAD_REQUEST, e.getMessage());
        }
        return response;
    }

    public static LocalDateTime convertToLocalDate(String date) {
        Date dateToConvert = tryParseDate(date);
        return dateToConvert.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    public static LocalDateTime convertToLocalDate(Date date) {
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}

