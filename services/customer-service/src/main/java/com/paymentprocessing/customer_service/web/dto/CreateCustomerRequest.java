package com.paymentprocessing.customer_service.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateCustomerRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank String document) {
}
