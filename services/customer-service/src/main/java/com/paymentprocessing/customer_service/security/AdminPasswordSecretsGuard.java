package com.paymentprocessing.customer_service.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Refuses to start under the "prod" profile if ADMIN_PASSWORD was left at its insecure
 * docker-compose dev default. app.admin.password only exists in this service (it's the
 * one that bootstraps the seed admin account on first start - see AdminBootstrap), so
 * this check stays local rather than living in common-security's shared
 * ProductionSecretsGuard, which every other service would then need a value for too.
 * The JWT_SECRET check that IS common to every service comes from
 * common-security's JwtSecurityAutoConfiguration and runs independently of this class.
 */
@Component
@Profile("prod")
public class AdminPasswordSecretsGuard {

    private static final String INSECURE_ADMIN_PASSWORD = "admin12345";

    public AdminPasswordSecretsGuard(@Value("${app.admin.password}") String adminPassword) {
        if (INSECURE_ADMIN_PASSWORD.equals(adminPassword)) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD is still set to the insecure development default. " +
                            "Set a strong password via the ADMIN_PASSWORD environment variable before starting in the 'prod' profile.");
        }
    }
}
