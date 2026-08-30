package com.paymentprocessing.payment_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "account-service", url = "${account-service.base-url}")
public interface AccountClient {

    @GetMapping("/accounts/{id}")
    AccountDto getById(@PathVariable UUID id);
}
