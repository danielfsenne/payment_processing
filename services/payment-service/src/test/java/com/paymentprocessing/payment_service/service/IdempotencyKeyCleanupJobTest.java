package com.paymentprocessing.payment_service.service;

import com.paymentprocessing.payment_service.repository.IdempotencyKeyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyKeyCleanupJobTest {

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @InjectMocks
    private IdempotencyKeyCleanupJob job;

    @Test
    void deletesKeysOlderThanTheConfiguredTtl() {
        ReflectionTestUtils.setField(job, "keyTtl", Duration.ofHours(24));
        when(idempotencyKeyRepository.deleteByCreatedAtBefore(any(Instant.class))).thenReturn(3);

        Instant before = Instant.now();
        job.deleteExpiredKeys();
        Instant after = Instant.now();

        ArgumentCaptor<Instant> thresholdCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(idempotencyKeyRepository).deleteByCreatedAtBefore(thresholdCaptor.capture());
        Instant threshold = thresholdCaptor.getValue();

        assertThat(threshold).isBetween(before.minus(Duration.ofHours(24)), after.minus(Duration.ofHours(24)));
    }

    @Test
    void doesNothingElseWhenNoKeysAreExpired() {
        ReflectionTestUtils.setField(job, "keyTtl", Duration.ofHours(24));
        when(idempotencyKeyRepository.deleteByCreatedAtBefore(any(Instant.class))).thenReturn(0);

        job.deleteExpiredKeys();

        verify(idempotencyKeyRepository).deleteByCreatedAtBefore(any(Instant.class));
    }
}
