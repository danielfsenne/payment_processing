package com.paymentprocessing.customer_service.config;

import com.paymentprocessing.customer_service.domain.Customer;
import com.paymentprocessing.customer_service.domain.CustomerRole;
import com.paymentprocessing.customer_service.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * There is no registration flow for admins - POST /customers always creates a CUSTOMER.
 * The first (and only, unless promoted directly in the database) ADMIN account is
 * seeded here from config on startup, idempotently, so the system always has one way in.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrap implements ApplicationRunner {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.name}")
    private String adminName;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.document}")
    private String adminDocument;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (customerRepository.findByEmail(adminEmail).isPresent()) {
            return;
        }
        Customer admin = Customer.builder()
                .name(adminName)
                .email(adminEmail)
                .document(adminDocument)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(CustomerRole.ADMIN)
                .build();
        customerRepository.save(admin);
        log.info("Seeded admin customer {}", adminEmail);
    }
}
