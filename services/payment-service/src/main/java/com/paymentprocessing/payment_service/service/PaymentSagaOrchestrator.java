package com.paymentprocessing.payment_service.service;

import com.paymentprocessing.payment_service.client.CreateReservationRequest;
import com.paymentprocessing.payment_service.client.ReservationClient;
import com.paymentprocessing.payment_service.client.ReservationDto;
import com.paymentprocessing.payment_service.domain.Payment;
import com.paymentprocessing.payment_service.domain.PaymentStatus;
import com.paymentprocessing.payment_service.web.ConcurrentOperationException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates the payment saga: reserve balance -> process/capture -> settle,
 * notifying interested services along the way through the events already produced
 * by PaymentService.transition() (outbox -> RabbitMQ -> workers).
 *
 * This is intentionally NOT one big @Transactional method: each step commits its own
 * local state change (via PaymentService, whose methods are transactional) and may
 * call out over the network to account-service in between. Holding a single DB
 * transaction open across those network calls would be a classic distributed-saga
 * anti-pattern - locks held for the duration of a remote call. If a step fails, we
 * explicitly run the compensating action for whatever already succeeded (releasing
 * the reservation) rather than relying on rollback.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentSagaOrchestrator {

    private static final String SAGA_LOCK_PREFIX = "payment-saga:";
    private static final Duration SAGA_LOCK_TTL = Duration.ofSeconds(30);

    private final PaymentService paymentService;
    private final ReservationClient reservationClient;
    private final DistributedLockService lockService;

    public Payment process(UUID paymentId, boolean simulateProcessingFailure) {
        // Two concurrent /process calls for the same payment can both read CREATED
        // before either transitions it: the lock serializes the whole saga per payment
        // so the second call waits instead of double-reserving/double-capturing.
        Optional<String> token = lockService.tryLock(SAGA_LOCK_PREFIX + paymentId, SAGA_LOCK_TTL);
        if (token.isEmpty()) {
            throw new ConcurrentOperationException(
                    "Payment %s is already being processed by a concurrent request".formatted(paymentId));
        }
        try {
            return doProcess(paymentId, simulateProcessingFailure);
        } finally {
            lockService.unlock(SAGA_LOCK_PREFIX + paymentId, token.get());
        }
    }

    /**
     * Carries a payment that is stuck mid-saga (PROCESSING/AUTHORIZED/CAPTURED for
     * longer than expected, per PaymentReconciliationJob) the rest of the way to
     * SETTLED. Every downstream call here is idempotent (reserve is keyed by
     * paymentId, confirm/release short-circuit if already in the target state), so
     * it is always safe to re-issue whichever step didn't get a confirmed response
     * last time - it either replays a no-op or actually makes the progress that a
     * network blip previously swallowed.
     *
     * This never compensates and fails a payment past PROCESSING: once a reservation
     * may have been confirmed (real money moved), the only safe way forward is to
     * keep retrying that step, not to mark the payment FAILED without knowing
     * whether it already succeeded.
     */
    public void reconcile(UUID paymentId) {
        Optional<String> token = lockService.tryLock(SAGA_LOCK_PREFIX + paymentId, SAGA_LOCK_TTL);
        if (token.isEmpty()) {
            log.debug("Skipping reconciliation for payment {}: saga lock is held by an in-flight request", paymentId);
            return;
        }
        try {
            doReconcile(paymentId);
        } finally {
            lockService.unlock(SAGA_LOCK_PREFIX + paymentId, token.get());
        }
    }

    private void doReconcile(UUID paymentId) {
        Payment payment = paymentService.getById(paymentId);
        try {
            switch (payment.getStatus()) {
                case PROCESSING -> resumeFromProcessing(payment);
                case AUTHORIZED -> resumeFromAuthorized(payment);
                case CAPTURED -> resumeFromCaptured(payment);
                default -> log.debug("Payment {} is no longer stuck (status {}); nothing to reconcile",
                        paymentId, payment.getStatus());
            }
        } catch (FeignException.Conflict ex) {
            // Only reachable from PROCESSING, before any reservation could have been
            // confirmed - no money has moved yet, so failing outright is safe.
            log.warn("Reconciliation step 'reserve balance' failed for payment {}: insufficient funds", paymentId);
            paymentService.markFailed(paymentId, "Insufficient funds");
        } catch (FeignException ex) {
            log.warn("Reconciliation for payment {} (status {}) could not complete, will retry on next run: {}",
                    paymentId, payment.getStatus(), ex.getMessage());
        }
    }

    private Payment doProcess(UUID paymentId, boolean simulateProcessingFailure) {
        Payment payment = paymentService.getById(paymentId);
        if (payment.getStatus() != PaymentStatus.CREATED) {
            throw new IllegalStateException(
                    "Payment %s is not in CREATED state (currently %s); cannot start saga"
                            .formatted(paymentId, payment.getStatus()));
        }

        // Step 1: CREATED -> PROCESSING
        payment = paymentService.transition(paymentId, PaymentStatus.PROCESSING);

        // Step 2: reserve balance on the account
        ReservationDto reservation;
        try {
            reservation = reservationClient.reserve(
                    payment.getAccountId(),
                    new CreateReservationRequest(payment.getId(), payment.getAmount()));
        } catch (FeignException.Conflict ex) {
            log.warn("Saga step 'reserve balance' failed for payment {}: insufficient funds", paymentId);
            return paymentService.markFailed(paymentId, "Insufficient funds");
        }
        paymentService.attachReservation(paymentId, reservation.id());
        payment = paymentService.transition(paymentId, PaymentStatus.AUTHORIZED);

        // Step 3: processing/capture (simulated - this is where a real payment
        // processor integration would live)
        if (simulateProcessingFailure) {
            log.warn("Saga step 'processing' failed for payment {} (simulated); compensating reservation {}",
                    paymentId, reservation.id());
            reservationClient.release(reservation.id());
            return paymentService.markFailed(paymentId, "Processing failed (simulated)");
        }
        return resumeFromAuthorized(payment);
    }

    private Payment resumeFromProcessing(Payment payment) {
        ReservationDto reservation = reservationClient.reserve(
                payment.getAccountId(),
                new CreateReservationRequest(payment.getId(), payment.getAmount()));
        paymentService.attachReservation(payment.getId(), reservation.id());
        Payment authorized = paymentService.transition(payment.getId(), PaymentStatus.AUTHORIZED);
        return resumeFromAuthorized(authorized);
    }

    private Payment resumeFromAuthorized(Payment payment) {
        if (payment.getReservationId() == null) {
            log.error("Payment {} is AUTHORIZED without a reservationId attached; cannot reconcile automatically",
                    payment.getId());
            return payment;
        }
        reservationClient.confirm(payment.getReservationId());
        Payment captured = paymentService.transition(payment.getId(), PaymentStatus.CAPTURED);
        return resumeFromCaptured(captured);
    }

    private Payment resumeFromCaptured(Payment payment) {
        // Settle. Notification is not orchestrated directly here - it is driven by
        // the outbox event this transition produces, consumed asynchronously by
        // notification-worker.
        return paymentService.transition(payment.getId(), PaymentStatus.SETTLED);
    }
}
