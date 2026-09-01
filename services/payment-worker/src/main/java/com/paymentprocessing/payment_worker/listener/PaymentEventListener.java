package com.paymentprocessing.payment_worker.listener;

import com.paymentprocessing.payment_worker.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void handle(String payload) {
        IncomingPaymentEvent event = objectMapper.readValue(payload, IncomingPaymentEvent.class);
        log.info("payment-worker processing payment {} ({} -> {})",
                event.paymentId(), event.fromStatus(), event.toStatus());
    }
}
