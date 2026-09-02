package com.paymentprocessing.payment_service.web;

public class ConcurrentOperationException extends RuntimeException {

    public ConcurrentOperationException(String message) {
        super(message);
    }
}
