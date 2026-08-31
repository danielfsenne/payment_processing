package com.paymentprocessing.payment_service.service;

public record PaymentCreationResult(int status, String body, boolean replayed) {
}
