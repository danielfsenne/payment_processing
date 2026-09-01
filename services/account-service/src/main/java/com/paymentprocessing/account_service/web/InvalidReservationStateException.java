package com.paymentprocessing.account_service.web;

import com.paymentprocessing.account_service.domain.ReservationStatus;

import java.util.UUID;

public class InvalidReservationStateException extends RuntimeException {

    public InvalidReservationStateException(UUID reservationId, ReservationStatus current, String action) {
        super("Cannot %s reservation %s in state %s".formatted(action, reservationId, current));
    }
}
