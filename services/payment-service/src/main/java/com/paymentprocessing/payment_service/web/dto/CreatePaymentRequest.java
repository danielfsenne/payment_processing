package com.paymentprocessing.payment_service.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentRequest(
        @NotNull UUID customerId,
        @NotNull UUID accountId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotNull String currency) {
}
