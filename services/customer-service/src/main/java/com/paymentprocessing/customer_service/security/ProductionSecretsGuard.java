package com.paymentprocessing.customer_service.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Refuses to start under the "prod" profile if JWT_SECRET/ADMIN_PASSWORD were left at
 * their insecure docker-compose dev defaults, or if the JWT secret is too short for HS256.
 * Dev/default profile is untouched so local docker-compose usage keeps working as-is.
 */
@Component
@Profile("prod")
public class ProductionSecretsGuard {

    private static final String INSECURE_JWT_SECRET = "dev-only-shared-secret-change-me-in-prod-32bytes";
    private static final String INSECURE_ADMIN_PASSWORD = "admin12345";
    private static final int MIN_JWT_SECRET_BYTES = 32;

    public ProductionSecretsGuard(@Value("${security.jwt.secret}") String jwtSecret,
                                   @Value("${app.admin.password}") String adminPassword) {
        if (INSECURE_JWT_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException(
                    "JWT_SECRET is still set to the insecure development default. " +
                            "Set a unique secret via the JWT_SECRET environment variable before starting in the 'prod' profile.");
        }
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_JWT_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes long for HS256.");
        }
        if (INSECURE_ADMIN_PASSWORD.equals(adminPassword)) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD is still set to the insecure development default. " +
                            "Set a strong password via the ADMIN_PASSWORD environment variable before starting in the 'prod' profile.");
        }
    }
}
