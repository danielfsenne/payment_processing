package com.paymentprocessing.account_service.web.dto;

import com.paymentprocessing.account_service.domain.Account;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        UUID customerId,
        BigDecimal balance,
        BigDecimal reservedAmount,
        BigDecimal availableBalance,
        String currency,
        Instant createdAt) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getCustomerId(),
                account.getBalance(),
                account.getReservedAmount(),
                account.getAvailableBalance(),
                account.getCurrency(),
                account.getCreatedAt());
    }
}
