package com.paymentprocessing.payment_service.service;

import com.paymentprocessing.payment_service.domain.OutboxEvent;
import com.paymentprocessing.payment_service.domain.OutboxStatus;
import com.paymentprocessing.payment_service.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OutboxPublisher outboxPublisher;

    private OutboxEvent newEvent(int attempts) {
        return OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("PAYMENT")
                .aggregateId(UUID.randomUUID())
                .eventType("PAYMENT_CREATED")
                .routingKey("payment.created")
                .payload("{}")
                .status(OutboxStatus.PENDING)
                .attempts(attempts)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void staysPendingAndKeepsCountingAttemptsWhileBelowLimit() {
        ReflectionTestUtils.setField(outboxPublisher, "maxAttempts", 10);
        OutboxEvent event = newEvent(0);
        doThrow(new AmqpException("broker unreachable") {})
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        outboxPublisher.publish(event);

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getLastError()).contains("broker unreachable");
        verify(outboxEventRepository).save(event);
    }

    @Test
    void movesToFailedOnceMaxAttemptsIsReached() {
        ReflectionTestUtils.setField(outboxPublisher, "maxAttempts", 10);
        OutboxEvent event = newEvent(9);
        doThrow(new AmqpException("broker unreachable") {})
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        outboxPublisher.publish(event);

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(event.getAttempts()).isEqualTo(10);
        verify(outboxEventRepository).save(event);
    }

    @Test
    void marksEventPublishedOnSuccess() {
        OutboxEvent event = newEvent(2);

        outboxPublisher.publish(event);

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isNotNull();
        verify(outboxEventRepository).save(event);
    }
}
