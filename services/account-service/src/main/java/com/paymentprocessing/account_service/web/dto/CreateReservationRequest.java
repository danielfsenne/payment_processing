package com.paymentprocessing.account_service.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateReservationRequest(
        @NotNull UUID paymentId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount) {
}
