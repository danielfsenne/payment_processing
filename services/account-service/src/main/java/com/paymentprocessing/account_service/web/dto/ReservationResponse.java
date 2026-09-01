package com.paymentprocessing.account_service.web.dto;

import com.paymentprocessing.account_service.domain.Reservation;
import com.paymentprocessing.account_service.domain.ReservationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReservationResponse(
        UUID id,
        UUID accountId,
        UUID paymentId,
        BigDecimal amount,
        ReservationStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getAccountId(),
                reservation.getPaymentId(),
                reservation.getAmount(),
                reservation.getStatus(),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt());
    }
}
