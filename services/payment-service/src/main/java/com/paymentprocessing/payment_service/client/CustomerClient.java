package com.paymentprocessing.payment_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "customer-service", url = "${customer-service.base-url}")
public interface CustomerClient {

    @GetMapping("/customers/{id}")
    CustomerDto getById(@PathVariable UUID id);
}
