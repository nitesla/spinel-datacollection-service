package com.spinel.datacollection.service.integrations.paystack.enums;

import com.spinel.framework.exceptions.NotFoundException;
import com.spinel.framework.utils.CustomResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum PayStackTransactionStatus {
    FAILED("failed"),
    SUCCESS("success"),
    ABANDONED("abandoned")
    ;

    private final String alias;

    public static void isValidPayStackTransactionStatus(String status) {
        Arrays.stream(values()).parallel().filter(value -> value.alias.equals(status))
                .findFirst().orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, "Not a valid status"));
    }
}
