package com.paymentprocessing.payment_service.service;

import com.paymentprocessing.payment_service.config.RabbitMQConfig;
import com.paymentprocessing.payment_service.domain.OutboxEvent;
import com.paymentprocessing.payment_service.domain.OutboxStatus;
import com.paymentprocessing.payment_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Polls outbox_events for rows still PENDING and publishes them to RabbitMQ. A row
 * only ever becomes PENDING inside the same DB transaction as the aggregate change
 * it describes, so if the broker is unreachable this loop simply leaves rows PENDING
 * and retries them on the next tick - no event creation is ever lost, it's just
 * delayed until the broker is back.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelay = 2000)
    public void publishPending() {
        List<OutboxEvent> pending = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        for (OutboxEvent event : pending) {
            publish(event);
        }
    }

    @Transactional
    void publish(OutboxEvent event) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.PAYMENTS_EXCHANGE, event.getRoutingKey(), event.getPayload());
            event.setStatus(OutboxStatus.PUBLISHED);
            event.setPublishedAt(Instant.now());
            log.info("Published outbox event {} ({}) after {} attempt(s)",
                    event.getId(), event.getRoutingKey(), event.getAttempts() + 1);
        } catch (AmqpException ex) {
            event.setAttempts(event.getAttempts() + 1);
            log.warn("Failed to publish outbox event {} ({}), attempt {}: {}",
                    event.getId(), event.getRoutingKey(), event.getAttempts(), ex.getMessage());
        }
        outboxEventRepository.save(event);
    }
}
