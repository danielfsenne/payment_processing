package com.paymentprocessing.payment_service.service;

import com.paymentprocessing.payment_service.client.AccountClient;
import com.paymentprocessing.payment_service.client.CustomerClient;
import com.paymentprocessing.payment_service.domain.IdempotencyKey;
import com.paymentprocessing.payment_service.domain.Payment;
import com.paymentprocessing.payment_service.domain.PaymentEvent;
import com.paymentprocessing.payment_service.domain.PaymentStatus;
import com.paymentprocessing.payment_service.repository.PaymentEventRepository;
import com.paymentprocessing.payment_service.repository.PaymentRepository;
import com.paymentprocessing.payment_service.statemachine.PaymentStateMachine;
import com.paymentprocessing.payment_service.web.IdempotencyConflictException;
import com.paymentprocessing.payment_service.web.ResourceNotFoundException;
import com.paymentprocessing.payment_service.web.dto.CreatePaymentRequest;
import com.paymentprocessing.payment_service.web.dto.PaymentResponse;
import tools.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final PaymentStateMachine stateMachine;
    private final CustomerClient customerClient;
    private final AccountClient accountClient;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @Transactional
    public PaymentCreationResult create(CreatePaymentRequest request, String idempotencyKey) {
        String fingerprint = idempotencyService.fingerprint(request);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
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

        String body = writeJson(PaymentResponse.from(payment));
        int status = HttpStatus.CREATED.value();

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyService.record(idempotencyKey, fingerprint, status, PaymentResponse.from(payment));
        }

        return new PaymentCreationResult(status, body, false);
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

    @Transactional
    public Payment transition(UUID id, PaymentStatus to) {
        Payment payment = getById(id);
        PaymentStatus from = payment.getStatus();
        stateMachine.transition(payment, to);
        Payment saved = paymentRepository.save(payment);
        recordEvent(saved, "TRANSITIONED", from, to);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<PaymentEvent> getEvents(UUID paymentId) {
        getById(paymentId);
        return paymentEventRepository.findByPaymentIdOrderByCreatedAtAsc(paymentId);
    }

    private Payment createPayment(CreatePaymentRequest request) {
        requireCustomerExists(request.customerId());
        requireAccountExists(request.accountId());

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

    private void requireCustomerExists(UUID customerId) {
        try {
            customerClient.getById(customerId);
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Customer not found: " + customerId);
        }
    }

    private void requireAccountExists(UUID accountId) {
        try {
            accountClient.getById(accountId);
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Account not found: " + accountId);
        }
    }
}
