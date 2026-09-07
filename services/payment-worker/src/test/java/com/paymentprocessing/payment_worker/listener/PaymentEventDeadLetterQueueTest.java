package com.paymentprocessing.payment_worker.listener;

import com.paymentprocessing.payment_worker.config.RabbitMQConfig;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the retry+DLQ topology actually works end to end against a real broker: a
 * message that can never be handled successfully (invalid JSON, so
 * PaymentEventListener.handle always throws) must exhaust its local retries and land in
 * the dead-letter queue - not be redelivered forever, and not be silently dropped.
 */
@Testcontainers
@SpringBootTest
class PaymentEventDeadLetterQueueTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management-alpine"));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitmq::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitmq::getAdminPassword);
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void aMessageThatNeverParsesEndsUpInTheDeadLetterQueueAfterRetriesAreExhausted() {
        rabbitTemplate.convertAndSend(RabbitMQConfig.PAYMENTS_EXCHANGE, "payment.created", "not-valid-json");

        Message dead = pollDeadLetterQueue(Duration.ofSeconds(20));

        assertThat(dead).as("message should have been routed to the DLQ after exhausting retries").isNotNull();
        assertThat(new String(dead.getBody())).isEqualTo("not-valid-json");
    }

    private Message pollDeadLetterQueue(Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            Message message = rabbitTemplate.receive(RabbitMQConfig.DLQ);
            if (message != null) {
                return message;
            }
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }
}
