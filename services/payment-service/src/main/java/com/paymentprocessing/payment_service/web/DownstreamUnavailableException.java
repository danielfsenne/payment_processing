package com.paymentprocessing.payment_service.web;

/**
 * Thrown by a Feign fallback when the circuit breaker for that downstream is open (or
 * the call itself failed) - distinct from ResourceNotFoundException, which means the
 * downstream answered and said "no such resource". This means the downstream couldn't
 * be asked at all.
 */
public class DownstreamUnavailableException extends RuntimeException {

    public DownstreamUnavailableException(String service) {
        super(service + " is currently unavailable; please retry shortly");
    }
}
