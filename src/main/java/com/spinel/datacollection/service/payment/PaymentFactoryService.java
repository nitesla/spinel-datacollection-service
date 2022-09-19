package com.spinel.datacollection.service.payment;

import com.spinel.datacollection.service.helper.IntegratedPaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class PaymentFactoryService {

    private final Map<IntegratedPaymentService, PaymentService> paymentMap = new HashMap<>();

    @Autowired
    public PaymentFactoryService(
            @Qualifier("payStackServiceImpl") PaymentService payStackServiceImpl
    ) {
        addPayment(IntegratedPaymentService.PAYSTACK, payStackServiceImpl);
    }
    
    private void addPayment(IntegratedPaymentService integratedPaymentService, PaymentService paymentService) {
        paymentMap.put(integratedPaymentService, paymentService);
    }

    public PaymentService getPaymentService(IntegratedPaymentService paymentService) {
        return paymentMap.get(paymentService);
    }
    
    
}
