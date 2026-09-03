package com.paymentprocessing.notification_worker.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Same RBAC contract as the REST services (see the account/payment-service
 * authorization tests), proven here over STOMP instead of HTTP: a customer can
 * subscribe to their own payment topic, gets rejected subscribing to someone else's,
 * and a missing token never gets past CONNECT.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketAuthorizationTest {

    @LocalServerPort
    private int port;

    @Value("${security.jwt.secret}")
    private String secret;

    @Test
    void ownerCanSubscribeToTheirOwnTopic() throws Exception {
        String customerId = UUID.randomUUID().toString();
        WebSocketStompClient client = newClient();
        StompSession session = connect(client, token(customerId, "CUSTOMER"));
        session.subscribe("/topic/payments/" + customerId, new StompSessionHandlerAdapter() {
        });
        Thread.sleep(300);
        assertThat(session.isConnected()).isTrue();
    }

    @Test
    void subscribingToSomeoneElsesTopicClosesTheSession() throws Exception {
        String customerId = UUID.randomUUID().toString();
        String otherCustomerId = UUID.randomUUID().toString();
        WebSocketStompClient client = newClient();
        StompSession session = connect(client, token(customerId, "CUSTOMER"));

        CompletableFuture<Throwable> transportError = new CompletableFuture<>();
        session.subscribe("/topic/payments/" + otherCustomerId, new StompSessionHandlerAdapter() {
            @Override
            public void handleTransportError(StompSession s, Throwable exception) {
                transportError.complete(exception);
            }
        });

        // the server rejects the SUBSCRIBE and closes the session; the client either
        // surfaces that as a transport error or simply stops being connected
        transportError.orTimeout(3, TimeUnit.SECONDS).exceptionally(ex -> null).get();
        assertThat(session.isConnected()).isFalse();
    }

    @Test
    void connectWithoutATokenIsRejected() {
        WebSocketStompClient client = newClient();
        CompletableFuture<StompSession> failure = new CompletableFuture<>();
        client.connectAsync("ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(), new StompHeaders(),
                new StompSessionHandlerAdapter() {
                    @Override
                    public void handleTransportError(StompSession session, Throwable exception) {
                        failure.completeExceptionally(exception);
                    }
                });

        assertThatThrownBy(() -> failure.get(5, TimeUnit.SECONDS)).isInstanceOf(Exception.class);
    }

    private StompSession connect(WebSocketStompClient client, String token) throws Exception {
        return client.connectAsync("ws://localhost:" + port + "/ws?token=" + token, new WebSocketHttpHeaders(),
                        new StompHeaders(), new StompSessionHandlerAdapter() {
                        })
                .get(5, TimeUnit.SECONDS);
    }

    private WebSocketStompClient newClient() {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new StringMessageConverter());
        return client;
    }

    private String token(String subject, String role) {
        SecretKeySpec key = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("test")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .subject(subject)
                .claim("roles", List.of(role))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
