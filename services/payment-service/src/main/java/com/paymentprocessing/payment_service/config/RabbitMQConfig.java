package com.paymentprocessing.payment_service.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String PAYMENTS_EXCHANGE = "payments.exchange";

    @Bean
    public TopicExchange paymentsExchange() {
        return new TopicExchange(PAYMENTS_EXCHANGE, true, false);
    }
}
