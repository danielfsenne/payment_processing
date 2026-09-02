package com.paymentprocessing.payment_service.service;

import com.paymentprocessing.payment_service.web.ConcurrentOperationException;
import com.paymentprocessing.payment_service.web.dto.CreatePaymentRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private LookupCacheService lookupCacheService;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private OutboxService outboxService;

    @Mock
    private DistributedLockService lockService;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void rejectsConcurrentCreateRequestsSharingTheSameIdempotencyKey() {
        String idempotencyKey = "8f7a9-duplicate";
        CreatePaymentRequest request = new CreatePaymentRequest(
                UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, "BRL");
        when(lockService.tryLock(eq("idempotency-key:" + idempotencyKey), any(Duration.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.create(request, idempotencyKey))
                .isInstanceOf(ConcurrentOperationException.class);

        verify(idempotencyService, never()).find(any());
        verify(lookupCacheService, never()).customerExists(any());
    }
}
