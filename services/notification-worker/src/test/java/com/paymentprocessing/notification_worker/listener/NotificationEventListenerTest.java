package com.paymentprocessing.notification_worker.listener;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void publishesEachConsumedEventToItsCustomerTopic() {
        NotificationEventListener listener = new NotificationEventListener(objectMapper, messagingTemplate);
        String customerId = "8f7a9b1e-1111-4444-8888-000000000001";
        String paymentId = "8f7a9b1e-2222-4444-8888-000000000002";
        String accountId = "8f7a9b1e-3333-4444-8888-000000000003";
        String payload = """
                {
                  "paymentId": "%s",
                  "customerId": "%s",
                  "accountId": "%s",
                  "amount": 100.00,
                  "currency": "BRL",
                  "eventType": "TRANSITIONED",
                  "fromStatus": "CREATED",
                  "toStatus": "PROCESSING",
                  "occurredAt": "2026-01-01T00:00:00Z"
                }
                """.formatted(paymentId, customerId, accountId);

        listener.handle(payload);

        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<IncomingPaymentEvent> eventCaptor = ArgumentCaptor.forClass(IncomingPaymentEvent.class);
        verify(messagingTemplate).convertAndSend(destinationCaptor.capture(), eventCaptor.capture());

        assertThat(destinationCaptor.getValue()).isEqualTo("/topic/payments/" + customerId);
        assertThat(eventCaptor.getValue().paymentId().toString()).isEqualTo(paymentId);
        assertThat(eventCaptor.getValue().toStatus()).isEqualTo("PROCESSING");
    }
}
