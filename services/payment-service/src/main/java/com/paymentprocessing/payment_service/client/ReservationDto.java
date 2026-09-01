package com.paymentprocessing.payment_service.client;

import java.math.BigDecimal;
import java.util.UUID;

public record ReservationDto(UUID id, UUID accountId, UUID paymentId, BigDecimal amount, String status) {
}
