package com.paymentprocessing.payment_service.web.dto;

import com.paymentprocessing.payment_service.domain.PaymentEvent;
import com.paymentprocessing.payment_service.domain.PaymentStatus;

import java.time.Instant;
import java.util.UUID;

public record PaymentEventResponse(
        UUID id,
        String eventType,
        PaymentStatus fromStatus,
        PaymentStatus toStatus,
        Instant createdAt) {

    public static PaymentEventResponse from(PaymentEvent event) {
        return new PaymentEventResponse(
                event.getId(),
                event.getEventType(),
                event.getFromStatus(),
                event.getToStatus(),
                event.getCreatedAt());
    }
}
