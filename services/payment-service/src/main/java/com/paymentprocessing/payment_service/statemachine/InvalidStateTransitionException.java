package com.paymentprocessing.payment_service.statemachine;

import com.paymentprocessing.payment_service.domain.PaymentStatus;

public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(PaymentStatus from, PaymentStatus to) {
        super("Invalid payment state transition: " + from + " -> " + to);
    }
}
