package com.paymentprocessing.payment_worker.listener;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PaymentEventListener currently only logs a consumed event (it's the seam where a
 * real downstream integration would live), so there's no side effect to assert on for
 * the happy path - the meaningful behavior to pin down is that a well-formed payload
 * is parsed without error (so the message gets acked) and a malformed one throws (so
 * RabbitMQConfig's retry/DLQ topology - see PaymentEventDeadLetterQueueTest for that
 * path end to end against a real broker - actually gets exercised).
 */
class PaymentEventListenerTest {

    private final PaymentEventListener listener = new PaymentEventListener(new ObjectMapper());

    @Test
    void handlesAWellFormedPaymentEventWithoutThrowing() {
        String payload = """
                {
                  "paymentId": "8f7a9b1e-2222-4444-8888-000000000002",
                  "customerId": "8f7a9b1e-1111-4444-8888-000000000001",
                  "accountId": "8f7a9b1e-3333-4444-8888-000000000003",
                  "amount": 100.00,
                  "currency": "BRL",
                  "eventType": "TRANSITIONED",
                  "fromStatus": "CREATED",
                  "toStatus": "PROCESSING",
                  "occurredAt": "2026-01-01T00:00:00Z"
                }
                """;

        assertThatCode(() -> listener.handle(payload)).doesNotThrowAnyException();
    }

    @Test
    void throwsOnAMalformedPayloadSoTheContainerRetriesItInsteadOfSilentlyAckingGarbage() {
        assertThatThrownBy(() -> listener.handle("not-valid-json"));
    }
}
