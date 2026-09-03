package com.paymentprocessing.payment_service.service;

import com.paymentprocessing.payment_service.domain.IdempotencyKey;
import com.paymentprocessing.payment_service.domain.Payment;
import com.paymentprocessing.payment_service.domain.PaymentEvent;
import com.paymentprocessing.payment_service.domain.PaymentStatus;
import com.paymentprocessing.payment_service.repository.PaymentEventRepository;
import com.paymentprocessing.payment_service.repository.PaymentRepository;
import com.paymentprocessing.payment_service.statemachine.PaymentStateMachine;
import com.paymentprocessing.payment_service.web.ConcurrentOperationException;
import com.paymentprocessing.payment_service.web.IdempotencyConflictException;
import com.paymentprocessing.payment_service.web.ResourceNotFoundException;
import com.paymentprocessing.payment_service.web.dto.CreatePaymentRequest;
import com.paymentprocessing.payment_service.web.dto.PaymentResponse;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String IDEMPOTENCY_LOCK_PREFIX = "idempotency-key:";
    private static final Duration IDEMPOTENCY_LOCK_TTL = Duration.ofSeconds(10);

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final PaymentStateMachine stateMachine;
    private final LookupCacheService lookupCacheService;
    private final IdempotencyService idempotencyService;
    private final OutboxService outboxService;
    private final DistributedLockService lockService;
    private final ObjectMapper objectMapper;

    @Transactional
    public PaymentCreationResult create(CreatePaymentRequest request, String idempotencyKey) {
        String key = normalizeKey(idempotencyKey);
        if (key == null) {
            return doCreate(request, null);
        }

        // Two requests can carry the same Idempotency-Key and race each other: both can
        // miss the "already exists" check below before either has committed its insert.
        // The lock serializes them so the second one waits behind the first instead of
        // creating a duplicate payment.
        Optional<String> token = lockService.tryLock(IDEMPOTENCY_LOCK_PREFIX + key, IDEMPOTENCY_LOCK_TTL);
        if (token.isEmpty()) {
            throw new ConcurrentOperationException(
                    "Idempotency-Key '" + key + "' is already being processed by a concurrent request");
        }
        try {
            return doCreate(request, key);
        } finally {
            lockService.unlock(IDEMPOTENCY_LOCK_PREFIX + key, token.get());
        }
    }

    private PaymentCreationResult doCreate(CreatePaymentRequest request, String idempotencyKey) {
        String fingerprint = idempotencyService.fingerprint(request);

        if (idempotencyKey != null) {
            Optional<IdempotencyKey> existing = idempotencyService.find(idempotencyKey);
            if (existing.isPresent()) {
                IdempotencyKey record = existing.get();
                if (!idempotencyService.fingerprintMatches(record, fingerprint)) {
                    throw new IdempotencyConflictException(idempotencyKey);
                }
                return new PaymentCreationResult(record.getResponseStatus(), record.getResponseBody(), true);
            }
        }

        Payment payment = createPayment(request);
        recordEvent(payment, "CREATED", null, payment.getStatus());
        outboxService.recordCreated(payment);

        String body = writeJson(PaymentResponse.from(payment));
        int status = HttpStatus.CREATED.value();

        if (idempotencyKey != null) {
            idempotencyService.record(idempotencyKey, fingerprint, status, PaymentResponse.from(payment));
        }

        return new PaymentCreationResult(status, body, false);
    }

    private String normalizeKey(String idempotencyKey) {
        return (idempotencyKey != null && !idempotencyKey.isBlank()) ? idempotencyKey : null;
    }

    @Transactional(readOnly = true)
    public Payment getById(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Payment> list() {
        return paymentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Payment> listForCustomer(UUID customerId) {
        return paymentRepository.findByCustomerId(customerId);
    }

    @Transactional
    public Payment transition(UUID id, PaymentStatus to) {
        Payment payment = getById(id);
        PaymentStatus from = payment.getStatus();
        stateMachine.transition(payment, to);
        Payment saved = paymentRepository.save(payment);
        recordEvent(saved, "TRANSITIONED", from, to);
        outboxService.recordTransitioned(saved, from, to);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<PaymentEvent> getEvents(UUID paymentId) {
        getById(paymentId);
        return paymentEventRepository.findByPaymentIdOrderByCreatedAtAsc(paymentId);
    }

    @Transactional
    public Payment attachReservation(UUID id, UUID reservationId) {
        Payment payment = getById(id);
        payment.setReservationId(reservationId);
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment markFailed(UUID id, String reason) {
        Payment payment = getById(id);
        PaymentStatus from = payment.getStatus();
        stateMachine.transition(payment, PaymentStatus.FAILED);
        payment.setFailureReason(reason);
        Payment saved = paymentRepository.save(payment);
        recordEvent(saved, "TRANSITIONED", from, PaymentStatus.FAILED);
        outboxService.recordTransitioned(saved, from, PaymentStatus.FAILED);
        return saved;
    }

    private Payment createPayment(CreatePaymentRequest request) {
        lookupCacheService.customerExists(request.customerId());
        lookupCacheService.accountExists(request.accountId());

        Payment payment = Payment.builder()
                .customerId(request.customerId())
                .accountId(request.accountId())
                .amount(request.amount())
                .currency(request.currency())
                .status(PaymentStatus.CREATED)
                .build();
        return paymentRepository.save(payment);
    }

    private void recordEvent(Payment payment, String eventType, PaymentStatus from, PaymentStatus to) {
        PaymentEvent event = PaymentEvent.builder()
                .paymentId(payment.getId())
                .eventType(eventType)
                .fromStatus(from)
                .toStatus(to)
                .build();
        paymentEventRepository.save(event);
    }

    @SneakyThrows
    private String writeJson(Object value) {
        return objectMapper.writeValueAsString(value);
    }
}
