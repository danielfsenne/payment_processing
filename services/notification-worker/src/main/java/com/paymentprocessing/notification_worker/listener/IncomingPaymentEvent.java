package com.paymentprocessing.notification_worker.listener;

import java.math.BigDecimal;
import java.util.UUID;

public record IncomingPaymentEvent(
        UUID paymentId,
        UUID customerId,
        UUID accountId,
        BigDecimal amount,
        String currency,
        String eventType,
        String fromStatus,
        String toStatus,
        String occurredAt) {
}
