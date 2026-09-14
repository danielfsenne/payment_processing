package com.paymentprocessing.notification_worker.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * notification-worker has no customer-facing REST API, so HTTP itself stays open (the
 * WebSocket handshake and actuator both need to be reachable without a pre-flight
 * Spring Security check). Authorization instead happens per STOMP frame in
 * StompAuthChannelInterceptor, using the JwtDecoder bean that common-security's
 * JwtSecurityAutoConfiguration provides - same shared secret every other service
 * validates against, so a token minted by customer-service is honored here too.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
