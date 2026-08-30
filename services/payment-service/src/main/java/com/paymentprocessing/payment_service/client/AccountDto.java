package com.paymentprocessing.payment_service.client;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountDto(UUID id, UUID customerId, BigDecimal balance, String currency) {
}
