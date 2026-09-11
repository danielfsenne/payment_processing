package com.paymentprocessing.payment_service.service;

import com.paymentprocessing.payment_service.domain.Payment;
import com.paymentprocessing.payment_service.domain.PaymentStatus;
import com.paymentprocessing.payment_service.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentReconciliationJobTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentSagaOrchestrator sagaOrchestrator;

    @InjectMocks
    private PaymentReconciliationJob job;

    @Test
    void reconcilesEveryPaymentTheRepositoryReportsAsStuck() {
        ReflectionTestUtils.setField(job, "stuckAfter", Duration.ofMinutes(2));
        UUID stuckId = UUID.randomUUID();
        Payment stuck = Payment.builder().id(stuckId).status(PaymentStatus.AUTHORIZED)
                .updatedAt(Instant.now().minus(Duration.ofMinutes(5))).build();
        when(paymentRepository.findByStatusInAndUpdatedAtBefore(any(Collection.class), any(Instant.class)))
                .thenReturn(List.of(stuck));

        job.reconcileStuckPayments();

        verify(sagaOrchestrator).reconcile(stuckId);
    }

    @Test
    void looksUpPaymentsInTheThreeInFlightStatusesOlderThanTheConfiguredThreshold() {
        ReflectionTestUtils.setField(job, "stuckAfter", Duration.ofMinutes(2));
        when(paymentRepository.findByStatusInAndUpdatedAtBefore(any(Collection.class), any(Instant.class)))
                .thenReturn(List.of());

        job.reconcileStuckPayments();

        ArgumentCaptor<Collection<PaymentStatus>> statusesCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(paymentRepository).findByStatusInAndUpdatedAtBefore(statusesCaptor.capture(), any(Instant.class));
        assertThat(statusesCaptor.getValue())
                .containsExactlyInAnyOrder(PaymentStatus.PROCESSING, PaymentStatus.AUTHORIZED, PaymentStatus.CAPTURED);
    }
}
