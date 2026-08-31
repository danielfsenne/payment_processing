package com.paymentprocessing.payment_worker.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String PAYMENTS_EXCHANGE = "payments.exchange";
    public static final String QUEUE = "payment.worker.queue";

    @Bean
    public TopicExchange paymentsExchange() {
        return new TopicExchange(PAYMENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue paymentWorkerQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public Binding paymentWorkerBinding(Queue paymentWorkerQueue, TopicExchange paymentsExchange) {
        return BindingBuilder.bind(paymentWorkerQueue).to(paymentsExchange).with("payment.#");
    }
}
