package com.paymentprocessing.payment_service.client;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateReservationRequest(UUID paymentId, BigDecimal amount) {
}
