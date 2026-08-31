package com.paymentprocessing.payment_worker.listener;

import com.paymentprocessing.payment_worker.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PaymentEventListener {

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void handle(String payload) {
        log.info("payment-worker received event: {}", payload);
    }
}
