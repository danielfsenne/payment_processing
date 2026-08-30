package com.paymentprocessing.customer_service.web.dto;

import com.paymentprocessing.customer_service.domain.Customer;

import java.time.Instant;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String name,
        String email,
        String document,
        Instant createdAt) {

    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getDocument(),
                customer.getCreatedAt());
    }
}
