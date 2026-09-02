package com.paymentprocessing.account_service.service;

import com.paymentprocessing.account_service.domain.Account;
import com.paymentprocessing.account_service.repository.AccountRepository;
import com.paymentprocessing.account_service.web.InsufficientFundsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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

/**
 * Exercises ReservationService.reserve() against a real Postgres (not mocks) with many
 * threads racing to reserve funds from the same account. A mocked repository test cannot
 * catch this class of bug: it would happily return whatever balance you stub, hiding the
 * read-then-write race that only shows up when concurrent transactions actually hit the
 * same row. If the account only has enough balance for N of the M concurrent attempts,
 * exactly N must succeed and the account must never end up over-reserved.
 */
@Testcontainers
@SpringBootTest
class ReservationServiceConcurrencyTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void concurrentReservationsNeverOverdrawTheAccount() throws InterruptedException {
        BigDecimal reservationAmount = BigDecimal.valueOf(20);
        int attempts = 10;
        int expectedSuccesses = 5; // balance 100 / 20 per reservation

        Account account = accountRepository.save(Account.builder()
                .customerId(UUID.randomUUID())
                .balance(BigDecimal.valueOf(100))
                .reservedAmount(BigDecimal.ZERO)
                .currency("BRL")
                .build());

        List<Callable<Boolean>> tasks = new java.util.ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            UUID paymentId = UUID.randomUUID();
            tasks.add(() -> {
                try {
                    reservationService.reserve(account.getId(), paymentId, reservationAmount);
                    return true;
                } catch (InsufficientFundsException ex) {
                    return false;
                }
            });
        }

        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        try {
            List<Future<Boolean>> futures = pool.invokeAll(tasks);
            AtomicInteger successes = new AtomicInteger();
            for (Future<Boolean> future : futures) {
                try {
                    if (future.get()) {
                        successes.incrementAndGet();
                    }
                } catch (Exception ex) {
                    throw new AssertionError("Reservation task failed unexpectedly", ex);
                }
            }

            assertThat(successes.get()).isEqualTo(expectedSuccesses);
        } finally {
            pool.shutdown();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        }

        Account reloaded = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(reloaded.getReservedAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(reloaded.getAvailableBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
