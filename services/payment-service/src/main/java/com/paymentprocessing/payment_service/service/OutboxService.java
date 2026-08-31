package com.paymentprocessing.payment_service.service;

import com.paymentprocessing.payment_service.domain.OutboxEvent;
import com.paymentprocessing.payment_service.domain.Payment;
import com.paymentprocessing.payment_service.domain.PaymentStatus;
import com.paymentprocessing.payment_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private static final String AGGREGATE_TYPE = "PAYMENT";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void recordCreated(Payment payment) {
        record(payment, "CREATED", null, payment.getStatus());
    }

    @Transactional
    public void recordTransitioned(Payment payment, PaymentStatus from, PaymentStatus to) {
        record(payment, "TRANSITIONED", from, to);
    }

    @SneakyThrows
    private void record(Payment payment, String eventType, PaymentStatus from, PaymentStatus to) {
        PaymentEventPayload payload = new PaymentEventPayload(
                payment.getId(),
                payment.getCustomerId(),
                payment.getAccountId(),
                payment.getAmount(),
                payment.getCurrency(),
                eventType,
                from,
                to,
                Instant.now());

        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId(payment.getId())
                .eventType(eventType)
                .routingKey("payment." + eventType.toLowerCase())
                .payload(objectMapper.writeValueAsString(payload))
                .build();

        outboxEventRepository.save(event);
    }
}
