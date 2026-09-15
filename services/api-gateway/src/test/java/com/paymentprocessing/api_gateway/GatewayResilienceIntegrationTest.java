package com.paymentprocessing.api_gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the two resilience behaviors that GatewayConfigTest and FallbackControllerTest
 * only exercise in isolation, never wired together against real routes:
 *  - a downstream that's actually unreachable trips the route's CircuitBreaker filter
 *    and the client gets FallbackController's 503, not a hung connection or a raw 5xx;
 *  - the RequestRateLimiter default-filter actually rejects requests once the
 *    configured burst is exhausted, not just "the key resolver returns something".
 *
 * account-service's route is left pointed at a real (stub) backend and is what the
 * rate-limit test hits; payment-service's route is pointed at a closed local port
 * (nothing ever listens there) so a connection attempt fails immediately instead of
 * needing to wait out the full response-timeout.
 */
@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "RATE_LIMIT_REPLENISH_RATE=1",
                "RATE_LIMIT_BURST_CAPACITY=1"
        })
class GatewayResilienceIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private static HttpServer accountServiceStub;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        accountServiceStub = startStub();
        registry.add("ACCOUNT_SERVICE_URL", () -> "http://localhost:" + accountServiceStub.getAddress().getPort());
        registry.add("PAYMENT_SERVICE_URL", () -> "http://localhost:" + closedPort());
    }

    private static HttpServer startStub() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                byte[] body = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** A port nothing is listening on: bind then immediately release it. */
    private static int closedPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @AfterAll
    static void stopStub() {
        accountServiceStub.stop(0);
    }

    @LocalServerPort
    private int port;

    private WebTestClient client() {
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void fallsBackToServiceUnavailableWhenTheDownstreamIsUnreachable() {
        client().get().uri("/api/payments/123").exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody()
                .jsonPath("$.error").isEqualTo("service_unavailable")
                .jsonPath("$.message").value(message -> assertThat((String) message).contains("payment-service"));
    }

    @Test
    void rejectsRequestsOnceTheRateLimitBurstIsExhausted() {
        List<Integer> statuses = new CopyOnWriteArrayList<>();
        for (int i = 0; i < 5; i++) {
            int status = client().get().uri("/api/accounts/123").exchange()
                    .returnResult(Void.class)
                    .getStatus()
                    .value();
            statuses.add(status);
        }

        assertThat(statuses).as("burst capacity of 1 should let the first request through and reject the rest")
                .contains(429);
    }
}
