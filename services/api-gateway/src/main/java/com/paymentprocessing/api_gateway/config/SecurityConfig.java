package com.paymentprocessing.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * The gateway itself enforces no authentication or authorization - it just forwards
 * the Authorization header untouched, and each downstream service (account-service,
 * payment-service, customer-service) independently validates the JWT and enforces its
 * own rules. GatewayConfig depends on spring-boot-starter-security-oauth2-resource-server
 * only for its ReactiveJwtDecoder bean (used to pick a per-user rate-limit key), but
 * that dependency also pulls in Spring Security's reactive auto-configuration, which -
 * absent an explicit SecurityWebFilterChain bean - defaults to requiring a valid bearer
 * token on every request. Without this class the gateway would 401/403 everything,
 * including requests it has no business authenticating itself.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange.anyExchange().permitAll())
                .build();
    }
}
