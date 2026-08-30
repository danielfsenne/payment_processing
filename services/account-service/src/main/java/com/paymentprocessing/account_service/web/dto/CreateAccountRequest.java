package com.paymentprocessing.account_service.web.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateAccountRequest(
        @NotNull UUID customerId,
        BigDecimal initialBalance,
        @NotNull String currency) {
}
