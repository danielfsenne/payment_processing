package com.paymentprocessing.payment_service.client;

import com.paymentprocessing.payment_service.web.DownstreamUnavailableException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CustomerClientFallback implements CustomerClient {

    @Override
    public CustomerDto getById(UUID id) {
        throw new DownstreamUnavailableException("customer-service");
    }
}
