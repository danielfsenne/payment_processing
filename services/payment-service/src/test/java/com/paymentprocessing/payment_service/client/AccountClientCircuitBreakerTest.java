package com.paymentprocessing.payment_service.client;

import com.paymentprocessing.payment_service.web.DownstreamUnavailableException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves the resilience4j wiring configured in application.yml (spring.cloud.openfeign
 * .circuitbreaker.enabled + resilience4j.circuitbreaker.instances.account-service)
 * actually trips: with account-service unreachable, repeated calls must open the
 * "account-service" circuit rather than each one hanging on its own connection attempt
 * or leaking a raw Feign/connection exception to the caller.
 */
@Testcontainers
@SpringBootTest
class AccountClientCircuitBreakerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        // nothing listens here - every call fails the same way a dead account-service would
        registry.add("account-service.base-url", () -> "http://localhost:19999");
    }

    @Autowired
    private AccountClient accountClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Test
    void repeatedFailuresOpenTheCircuitInsteadOfFailingForever() {
        for (int i = 0; i < 6; i++) {
            assertThatThrownBy(() -> accountClient.getById(UUID.randomUUID()))
                    .isInstanceOf(DownstreamUnavailableException.class);
        }

        // Spring Cloud OpenFeign names the circuit breaker after the Feign method
        // (e.g. "AccountClientgetByIdUUID"), not the @FeignClient name, so we match by
        // prefix instead of pinning the exact generated name.
        boolean anyOpen = circuitBreakerRegistry.getAllCircuitBreakers().stream()
                .filter(cb -> cb.getName().startsWith("AccountClient"))
                .peek(cb -> System.out.println("circuit breaker " + cb.getName() + " state=" + cb.getState()))
                .anyMatch(cb -> cb.getState() == CircuitBreaker.State.OPEN);

        assertThat(anyOpen)
                .as("the account-service circuit breaker should have opened after repeated failures")
                .isTrue();
    }
}
