package com.paymentprocessing.payment_service.service;

import com.paymentprocessing.payment_service.domain.Payment;
import com.paymentprocessing.payment_service.domain.PaymentStatus;
import com.paymentprocessing.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Finds payments that have sat in an in-flight saga state (PROCESSING, AUTHORIZED,
 * CAPTURED) for longer than expected - normally the sign of a network failure
 * between a saga step succeeding on the remote side and the local transition that
 * was supposed to follow it - and hands each one to
 * {@link PaymentSagaOrchestrator#reconcile(java.util.UUID)} to carry forward.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentReconciliationJob {

    private static final List<PaymentStatus> IN_FLIGHT_STATUSES =
            List.of(PaymentStatus.PROCESSING, PaymentStatus.AUTHORIZED, PaymentStatus.CAPTURED);

    private final PaymentRepository paymentRepository;
    private final PaymentSagaOrchestrator sagaOrchestrator;

    @Value("${reconciliation.stuck-after:PT2M}")
    private Duration stuckAfter;

    @Scheduled(fixedDelayString = "${reconciliation.interval:PT1M}")
    public void reconcileStuckPayments() {
        Instant threshold = Instant.now().minus(stuckAfter);
        List<Payment> stuck = paymentRepository.findByStatusInAndUpdatedAtBefore(IN_FLIGHT_STATUSES, threshold);
        for (Payment payment : stuck) {
            log.warn("Payment {} stuck in {} since {}; attempting reconciliation",
                    payment.getId(), payment.getStatus(), payment.getUpdatedAt());
            sagaOrchestrator.reconcile(payment.getId());
        }
    }
}
