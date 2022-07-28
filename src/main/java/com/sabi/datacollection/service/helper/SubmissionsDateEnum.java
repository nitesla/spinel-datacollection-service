package com.sabi.datacollection.service.helper;

import com.sabi.framework.exceptions.BadRequestException;
import com.sabi.framework.utils.CustomResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum SubmissionsDateEnum {
    MONTH("month", 12),
    WEEK("week", 7),
    DAY("day", 31)
    ;

    private final String value;
    private final int period;

    public static void validateDateEnum(String string) {
        Arrays.stream(values()).parallel().filter(value -> value.getValue().equals(string.toLowerCase()))
                .findFirst().orElseThrow(() -> new BadRequestException(CustomResponseCode.BAD_REQUEST, "Enter month or day"));
    }

    public static String lengthError() {
        return "length should not be greater than ";
    }

}
