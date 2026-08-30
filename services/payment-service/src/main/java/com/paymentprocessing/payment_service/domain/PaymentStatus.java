package com.paymentprocessing.payment_service.domain;

public enum PaymentStatus {
    CREATED,
    PROCESSING,
    AUTHORIZED,
    CAPTURED,
    SETTLED,
    FAILED,
    RETRYING
}
