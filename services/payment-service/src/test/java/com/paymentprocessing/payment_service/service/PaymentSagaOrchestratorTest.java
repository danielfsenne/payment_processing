package com.paymentprocessing.payment_service.service;

import com.paymentprocessing.payment_service.client.ReservationClient;
import com.paymentprocessing.payment_service.domain.Payment;
import com.paymentprocessing.payment_service.domain.PaymentStatus;
import com.paymentprocessing.payment_service.web.ConcurrentOperationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class PaymentSagaOrchestratorTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private ReservationClient reservationClient;

    @Mock
    private DistributedLockService lockService;

    @InjectMocks
    private PaymentSagaOrchestrator sagaOrchestrator;

    @Test
    void rejectsConcurrentProcessingOfTheSamePaymentInsteadOfDoubleReserving() {
        UUID paymentId = UUID.randomUUID();
        when(lockService.tryLock(eq("payment-saga:" + paymentId), any(Duration.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> sagaOrchestrator.process(paymentId, false))
                .isInstanceOf(ConcurrentOperationException.class);

        verify(paymentService, never()).getById(any());
        verify(reservationClient, never()).reserve(any(), any());
    }

    @Test
    void releasesTheLockEvenWhenTheSagaFailsPartway() {
        UUID paymentId = UUID.randomUUID();
        String token = "lock-token";
        when(lockService.tryLock(eq("payment-saga:" + paymentId), any(Duration.class)))
                .thenReturn(Optional.of(token));
        when(paymentService.getById(paymentId))
                .thenReturn(Payment.builder().id(paymentId).status(PaymentStatus.AUTHORIZED).build());

        assertThatThrownBy(() -> sagaOrchestrator.process(paymentId, false))
                .isInstanceOf(IllegalStateException.class);

        verify(lockService).unlock("payment-saga:" + paymentId, token);
    }
}
