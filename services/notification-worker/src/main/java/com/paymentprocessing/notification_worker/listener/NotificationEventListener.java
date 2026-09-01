package com.paymentprocessing.notification_worker.listener;

import com.paymentprocessing.notification_worker.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void handle(String payload) {
        IncomingPaymentEvent event = objectMapper.readValue(payload, IncomingPaymentEvent.class);
        log.info("notification-worker would notify customer {} about payment {} ({} -> {})",
                event.customerId(), event.paymentId(), event.fromStatus(), event.toStatus());
    }
}
