package com.paymentprocessing.api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.spec.SecretKeySpec;

/**
 * Rate limiting is applied to every route (spring.cloud.gateway.server.webflux.default-filters).
 * Requests carrying a valid JWT are partitioned per customer ("user:<sub>") rather than
 * per IP, so one customer hammering the API can't burn through the budget shared by
 * everyone else behind the same NAT/corporate network - and one customer switching IPs
 * can't dodge their own limit either. Anything without a valid token (public endpoints
 * like /api/auth/login, POST /api/customers, or a missing/forged/expired one) still
 * falls back to client IP, since that's the only signal available pre-authentication.
 *
 * The signature is actually verified here, not just decoded - a forged token must not
 * be able to mint unlimited fresh rate-limit buckets and bypass the IP-based fallback
 * entirely.
 */
@Configuration
public class GatewayConfig {

    @Bean
    public ReactiveJwtDecoder jwtDecoder(@Value("${security.jwt.secret}") String secret) {
        SecretKeySpec key = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
        return NimbusReactiveJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public KeyResolver rateLimitKeyResolver(ReactiveJwtDecoder jwtDecoder) {
        return exchange -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
                String token = authHeader.substring(7);
                return jwtDecoder.decode(token)
                        .map(jwt -> "user:" + jwt.getSubject())
                        .onErrorResume(ex -> Mono.just(clientIpKey(exchange)));
            }
            return Mono.just(clientIpKey(exchange));
        };
    }

    private String clientIpKey(ServerWebExchange exchange) {
        var remoteAddress = exchange.getRequest().getRemoteAddress();
        String ip = remoteAddress != null && remoteAddress.getAddress() != null
                ? remoteAddress.getAddress().getHostAddress()
                : "unknown";
        return "ip:" + ip;
    }
}
