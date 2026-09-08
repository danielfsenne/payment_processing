package com.paymentprocessing.payment_service.client;

import com.paymentprocessing.payment_service.web.DownstreamUnavailableException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AccountClientFallback implements AccountClient {

    @Override
    public AccountDto getById(UUID id) {
        throw new DownstreamUnavailableException("account-service");
    }
}
