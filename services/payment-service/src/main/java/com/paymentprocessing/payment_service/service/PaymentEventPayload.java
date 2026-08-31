package com.paymentprocessing.payment_service.service;

import com.paymentprocessing.payment_service.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentEventPayload(
        UUID paymentId,
        UUID customerId,
        UUID accountId,
        BigDecimal amount,
        String currency,
        String eventType,
        PaymentStatus fromStatus,
        PaymentStatus toStatus,
        Instant occurredAt) {
}
