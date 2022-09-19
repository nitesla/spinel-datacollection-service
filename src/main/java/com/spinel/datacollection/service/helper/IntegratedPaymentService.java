package com.spinel.datacollection.service.helper;


import com.spinel.framework.exceptions.NotFoundException;
import com.spinel.framework.utils.CustomResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum IntegratedPaymentService {
    PAYSTACK("paystack")
    ;

    private final String value;

    public static IntegratedPaymentService validatePaymentService(String name) {
        return Arrays.stream(values()).parallel().filter(value -> value.getValue().equals(name) )
                .findFirst().orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, "NOT FOUND: No integration found."));
    }


}
