package com.paymentprocessing.customer_service.web.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds, String refreshToken) {

    public static LoginResponse bearer(String accessToken, long expiresInSeconds, String refreshToken) {
        return new LoginResponse(accessToken, "Bearer", expiresInSeconds, refreshToken);
    }
}
