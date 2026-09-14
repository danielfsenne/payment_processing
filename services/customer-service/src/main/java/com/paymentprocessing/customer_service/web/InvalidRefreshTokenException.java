package com.paymentprocessing.customer_service.web;

public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("Refresh token is invalid, expired, or already used");
    }
}
