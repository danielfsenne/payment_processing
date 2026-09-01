package com.paymentprocessing.payment_service.web.dto;

/**
 * simulateProcessingFailure exists purely to make the saga's compensating path
 * (release the balance reservation, mark the payment FAILED) reproducible in tests
 * without wiring up a real payment processor integration.
 */
public record ProcessPaymentRequest(Boolean simulateProcessingFailure) {

    public static ProcessPaymentRequest defaultRequest() {
        return new ProcessPaymentRequest(false);
    }

    public boolean shouldSimulateFailure() {
        return Boolean.TRUE.equals(simulateProcessingFailure);
    }
}
