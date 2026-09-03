package com.paymentprocessing.notification_worker.security;

public class StompAuthorizationException extends RuntimeException {

    public StompAuthorizationException(String message) {
        super(message);
    }
}
