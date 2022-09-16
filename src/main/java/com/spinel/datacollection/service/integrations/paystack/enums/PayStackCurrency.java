package com.spinel.datacollection.service.integrations.paystack.enums;

import com.spinel.framework.exceptions.NotFoundException;
import com.spinel.framework.utils.CustomResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum PayStackCurrency {
    NIGERIA("NGN")
    ;

    private final String alias;

    public static void isValidPayStackCurrency(String currency) {
        Arrays.stream(values()).parallel().filter(value -> value.alias.equals(currency))
                .findFirst().orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, "Not a valid status"));
    }

}
