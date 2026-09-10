package com.paymentprocessing.notification_worker.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Refuses to start under the "prod" profile if JWT_SECRET was left at its insecure
 * docker-compose dev default, or is too short for HS256. Dev/default profile is untouched
 * so local docker-compose usage keeps working as-is.
 */
@Component
@Profile("prod")
public class ProductionSecretsGuard {

    private static final String INSECURE_JWT_SECRET = "dev-only-shared-secret-change-me-in-prod-32bytes";
    private static final int MIN_JWT_SECRET_BYTES = 32;

    public ProductionSecretsGuard(@Value("${security.jwt.secret}") String jwtSecret) {
        if (INSECURE_JWT_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException(
                    "JWT_SECRET is still set to the insecure development default. " +
                            "Set a unique secret via the JWT_SECRET environment variable before starting in the 'prod' profile.");
        }
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_JWT_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes long for HS256.");
        }
    }
}
