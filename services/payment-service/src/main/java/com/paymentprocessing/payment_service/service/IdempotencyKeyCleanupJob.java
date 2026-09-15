package com.paymentprocessing.payment_service.service;

import com.paymentprocessing.payment_service.repository.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * idempotency_keys exists purely to catch a client retrying a request it already sent
 * (network blip, timeout) - it only ever needs to cover the window a client might
 * plausibly retry in, not forever. Left unbounded, it grows by one row per payment
 * creation call indefinitely. This periodically deletes rows past that window so the
 * table stays proportional to recent traffic instead of the service's whole lifetime.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyKeyCleanupJob {

    private final IdempotencyKeyRepository idempotencyKeyRepository;

    @Value("${idempotency.key-ttl:PT24H}")
    private Duration keyTtl;

    @Scheduled(fixedDelayString = "${idempotency.cleanup-interval:PT1H}")
    @Transactional
    public void deleteExpiredKeys() {
        Instant threshold = Instant.now().minus(keyTtl);
        int deleted = idempotencyKeyRepository.deleteByCreatedAtBefore(threshold);
        if (deleted > 0) {
            log.info("Deleted {} expired idempotency key(s) older than {}", deleted, keyTtl);
        }
    }
}
