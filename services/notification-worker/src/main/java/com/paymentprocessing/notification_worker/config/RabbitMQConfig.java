package com.paymentprocessing.notification_worker.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.StatelessRetryOperationsInterceptor;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Dead-letter topology: a message that keeps failing exhausts its local retries
 * (see retryInterceptor) and is then nacked without requeue, which - because the
 * queue below declares x-dead-letter-exchange/routing-key - RabbitMQ routes
 * straight into the DLQ instead of redelivering it forever or dropping it.
 */
@Configuration
public class RabbitMQConfig {

    public static final String PAYMENTS_EXCHANGE = "payments.exchange";
    public static final String QUEUE = "notification.worker.queue";
    public static final String DLX = "payments.dlx";
    public static final String DLQ = "notification.worker.queue.dlq";

    @Bean
    public TopicExchange paymentsExchange() {
        return new TopicExchange(PAYMENTS_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    public Queue notificationWorkerQueue() {
        return QueueBuilder.durable(QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DLQ)
                .build();
    }

    @Bean
    public Queue notificationWorkerDeadLetterQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Binding notificationWorkerBinding(Queue notificationWorkerQueue, TopicExchange paymentsExchange) {
        return BindingBuilder.bind(notificationWorkerQueue).to(paymentsExchange).with("payment.#");
    }

    @Bean
    public Binding notificationWorkerDeadLetterBinding(Queue notificationWorkerDeadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(notificationWorkerDeadLetterQueue).to(deadLetterExchange).with(DLQ);
    }

    @Bean
    public MessageRecoverer messageRecoverer(RabbitTemplate rabbitTemplate) {
        return new RepublishMessageRecoverer(rabbitTemplate, DLX, DLQ);
    }

    @Bean
    public StatelessRetryOperationsInterceptor retryInterceptor(MessageRecoverer messageRecoverer) {
        return RetryInterceptorBuilder.stateless()
                .maxRetries(3)
                .backOffOptions(500, 2.0, 5000)
                .recoverer(messageRecoverer)
                .build();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, StatelessRetryOperationsInterceptor retryInterceptor) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAdviceChain(retryInterceptor);
        return factory;
    }
}
