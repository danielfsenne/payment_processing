package com.paymentprocessing.notification_worker.listener;

import com.paymentprocessing.notification_worker.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationEventListener {

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void handle(String payload) {
        log.info("notification-worker would notify customer for event: {}", payload);
    }
}
