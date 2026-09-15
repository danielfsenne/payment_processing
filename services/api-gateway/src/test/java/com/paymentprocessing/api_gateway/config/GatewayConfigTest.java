package com.paymentprocessing.api_gateway.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayConfigTest {

    private static final String SECRET = "test-only-secret-at-least-32-bytes-long";

    private final GatewayConfig config = new GatewayConfig();
    private final KeyResolver keyResolver = config.rateLimitKeyResolver(
            NimbusReactiveJwtDecoder.withSecretKey(new SecretKeySpec(SECRET.getBytes(), "HmacSHA256"))
                    .macAlgorithm(MacAlgorithm.HS256)
                    .build());

    private String validTokenFor(String subject) {
        SecretKeySpec key = new SecretKeySpec(SECRET.getBytes(), "HmacSHA256");
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(subject)
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofHours(1)))
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    private String tokenSignedWithADifferentSecret(String subject) {
        SecretKeySpec wrongKey = new SecretKeySpec("a-completely-different-secret-32-bytes!".getBytes(), "HmacSHA256");
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(wrongKey));
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(subject)
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofHours(1)))
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    @Test
    void resolvesToTheJwtSubjectWhenTheTokenIsValid() {
        String token = validTokenFor("customer-123");
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/payments").header(HttpHeaders.AUTHORIZATION, "Bearer " + token));

        String key = keyResolver.resolve(exchange).block();

        assertThat(key).isEqualTo("user:customer-123");
    }

    @Test
    void fallsBackToClientIpWhenThereIsNoAuthorizationHeader() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/auth/login")
                        .remoteAddress(new InetSocketAddress("203.0.113.5", 12345))
                        .build());

        String key = keyResolver.resolve(exchange).block();

        assertThat(key).isEqualTo("ip:203.0.113.5");
    }

    @Test
    void fallsBackToClientIpWhenTheTokenIsForgedWithAnUnknownSecret() {
        String forged = tokenSignedWithADifferentSecret("attacker");
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + forged)
                        .remoteAddress(new InetSocketAddress("198.51.100.9", 12345)));

        String key = keyResolver.resolve(exchange).block();

        assertThat(key).isEqualTo("ip:198.51.100.9");
    }

    @Test
    void fallsBackToClientIpWhenTheAuthorizationHeaderIsMalformed() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt")
                        .remoteAddress(new InetSocketAddress("198.51.100.9", 12345)));

        String key = keyResolver.resolve(exchange).block();

        assertThat(key).isEqualTo("ip:198.51.100.9");
    }
}
