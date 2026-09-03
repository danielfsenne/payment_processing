package com.paymentprocessing.customer_service.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCustomerRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank String document,
        @NotBlank @Size(min = 8) String password) {
}
