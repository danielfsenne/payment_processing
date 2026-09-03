package com.paymentprocessing.payment_service.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * customer-service and account-service both authorize by the caller's identity in the
 * JWT, not by "is this call coming from payment-service". So every outbound Feign call
 * this service makes on behalf of an incoming request (existence checks, the saga's
 * reserve/confirm/release) must carry that same request's Authorization header - this is
 * what actually makes authorization distributed instead of the gateway being the only
 * checkpoint. Only relevant for calls made on a request thread; the outbox publisher's
 * scheduled RabbitMQ writes don't go through Feign at all.
 */
@Configuration
public class FeignAuthRelayConfig {

    @Bean
    public RequestInterceptor authorizationRelayInterceptor() {
        return requestTemplate -> currentAuthorizationHeader()
                .ifPresent(value -> requestTemplate.header(HttpHeaders.AUTHORIZATION, value));
    }

    private Optional<String> currentAuthorizationHeader() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return Optional.empty();
        }
        return Optional.ofNullable(servletAttributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION));
    }
}
