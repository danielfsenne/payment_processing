package com.paymentprocessing.payment_service.service;

import com.paymentprocessing.payment_service.client.AccountClient;
import com.paymentprocessing.payment_service.client.AccountDto;
import com.paymentprocessing.payment_service.client.CustomerClient;
import com.paymentprocessing.payment_service.client.CustomerDto;
import com.paymentprocessing.payment_service.repository.PaymentRepository;
import com.paymentprocessing.payment_service.web.ConcurrentOperationException;
import com.paymentprocessing.payment_service.web.dto.CreatePaymentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Exercises PaymentService.create() against real Postgres and real Redis (not mocks) to
 * prove the Idempotency-Key lock added in DistributedLockService actually closes the race
 * it claims to: many concurrent requests carrying the same key must produce exactly one
 * Payment row, never one per request. A mocked RedisTemplate/lock test would only prove
 * the mock was called correctly, not that SET NX against a real Redis instance behaves
 * as an exclusive lock under real concurrency.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PaymentServiceIdempotencyConcurrencyTest {

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
    }

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockitoBean
    private CustomerClient customerClient;

    @MockitoBean
    private AccountClient accountClient;

    @Test
    void concurrentRequestsWithTheSameIdempotencyKeyCreateExactlyOnePayment() throws InterruptedException {
        UUID customerId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        when(customerClient.getById(any())).thenReturn(new CustomerDto(customerId, "Test", "t@example.com", "123"));
        when(accountClient.getById(any())).thenReturn(new AccountDto(accountId, customerId, BigDecimal.valueOf(1000), "BRL"));

        String idempotencyKey = "concurrent-create-" + UUID.randomUUID();
        CreatePaymentRequest request = new CreatePaymentRequest(customerId, accountId, BigDecimal.TEN, "BRL");
        int attempts = 8;

        List<Callable<Integer>> tasks = new java.util.ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            tasks.add(() -> {
                try {
                    return paymentService.create(request, idempotencyKey).status();
                } catch (ConcurrentOperationException ex) {
                    return -1;
                }
            });
        }

        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        try {
            List<Future<Integer>> futures = pool.invokeAll(tasks);
            AtomicInteger created = new AtomicInteger();
            for (Future<Integer> future : futures) {
                Integer status = future.get();
                if (status != null && status == 201) {
                    created.incrementAndGet();
                }
            }
            assertThat(created.get()).isGreaterThanOrEqualTo(1);
        } catch (Exception ex) {
            throw new AssertionError("Concurrent create task failed unexpectedly", ex);
        } finally {
            pool.shutdown();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        }

        assertThat(paymentRepository.count()).isEqualTo(1);
    }
}
