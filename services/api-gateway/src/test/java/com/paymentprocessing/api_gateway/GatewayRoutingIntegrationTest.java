package com.paymentprocessing.api_gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
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
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the route table in application.yml actually does what it says: each
 * /api/{service}/** predicate lands on the right downstream (stood up here as a tiny
 * JDK HttpServer stub instead of the real Spring Boot service, so this stays fast and
 * has no cross-service Docker dependency) with StripPrefix=1 removing only the leading
 * "/api" segment - the rest of the path (including the per-service prefix each
 * downstream controller owns, e.g. /accounts) is forwarded unchanged. None of this is
 * exercised by GatewayConfigTest (key resolver only) or FallbackControllerTest (the
 * fallback controller in isolation) - a typo in a route's Path predicate or the wrong
 * StripPrefix count would pass both of those and only show up here.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private static final AtomicReference<String> lastPaymentPath = new AtomicReference<>();
    private static final AtomicReference<String> lastAccountPath = new AtomicReference<>();
    private static final AtomicReference<String> lastCustomerPath = new AtomicReference<>();

    private static HttpServer paymentServiceStub;
    private static HttpServer accountServiceStub;
    private static HttpServer customerServiceStub;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        paymentServiceStub = stubServer(lastPaymentPath);
        accountServiceStub = stubServer(lastAccountPath);
        customerServiceStub = stubServer(lastCustomerPath);

        registry.add("PAYMENT_SERVICE_URL", () -> "http://localhost:" + paymentServiceStub.getAddress().getPort());
        registry.add("ACCOUNT_SERVICE_URL", () -> "http://localhost:" + accountServiceStub.getAddress().getPort());
        registry.add("CUSTOMER_SERVICE_URL", () -> "http://localhost:" + customerServiceStub.getAddress().getPort());
    }

    private static HttpServer stubServer(AtomicReference<String> lastPath) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                lastPath.set(exchange.getRequestURI().getPath());
                byte[] body = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
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

    @AfterAll
    static void stopStubs() {
        paymentServiceStub.stop(0);
        accountServiceStub.stop(0);
        customerServiceStub.stop(0);
    }

    @LocalServerPort
    private int port;

    private WebTestClient client() {
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    // StripPrefix=1 removes only the leading "/api" segment - the downstream
    // controllers each own their own top-level mapping (/accounts, /payments,
    // /customers, /auth), so that part of the path must reach them unstripped.

    @Test
    void routesPaymentRequestsToPaymentServiceWithOnlyTheApiPrefixStripped() {
        client().get().uri("/api/payments/123").exchange().expectStatus().isOk();

        assertThat(lastPaymentPath.get()).isEqualTo("/payments/123");
    }

    @Test
    void routesAccountRequestsToAccountServiceWithOnlyTheApiPrefixStripped() {
        client().get().uri("/api/accounts/123").exchange().expectStatus().isOk();

        assertThat(lastAccountPath.get()).isEqualTo("/accounts/123");
    }

    @Test
    void routesCustomerRequestsToCustomerServiceWithOnlyTheApiPrefixStripped() {
        client().get().uri("/api/customers/123").exchange().expectStatus().isOk();

        assertThat(lastCustomerPath.get()).isEqualTo("/customers/123");
    }

    @Test
    void routesAuthRequestsToCustomerServiceOnItsOwnRouteWithOnlyTheApiPrefixStripped() {
        client().post().uri("/api/auth/login").exchange().expectStatus().isOk();

        assertThat(lastCustomerPath.get()).isEqualTo("/auth/login");
    }
}
