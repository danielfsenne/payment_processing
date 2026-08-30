package com.paymentprocessing.payment_service.web.dto;

import com.paymentprocessing.payment_service.domain.PaymentStatus;
import jakarta.validation.constraints.NotNull;

public record TransitionRequest(@NotNull PaymentStatus to) {
}
