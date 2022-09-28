package com.spinel.datacollection.service.integrations.paystack.enums;

import com.spinel.framework.exceptions.NotFoundException;
import com.spinel.framework.utils.CustomResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum SubscriptionPlanInterval {
    HOURLY("hourly"),
    DAILY("daily"),
    WEEKLY("weekly"),
    MONTHLY("monthly"),
    QUARTERLY("quaterly"),
    ANNUALLY("annually")
    ;

    private final String alias;

    public static SubscriptionPlanInterval isValidPayStackInterval(String interval) {
        return Arrays.stream(values()).parallel().filter(value -> value.alias.equals(interval))
                .findFirst().orElseThrow(() -> new NotFoundException(CustomResponseCode.NOT_FOUND_EXCEPTION, "Not a valid status"));
    }
}
