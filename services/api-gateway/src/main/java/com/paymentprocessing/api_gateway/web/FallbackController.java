package com.paymentprocessing.api_gateway.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

/**
 * Reached when a route's CircuitBreaker filter is open or the downstream call
 * times out (spring.cloud.gateway...httpclient.response-timeout / the
 * resilience4j TimeLimiter attached to each route's circuit breaker). Without
 * this, a stuck downstream service would leave gateway requests hanging
 * indefinitely instead of failing fast with a clear "service unavailable".
 */
@RestController
public class FallbackController {

    @RequestMapping(path = "/fallback/{service}",
            method = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE })
    public Mono<ResponseEntity<Map<String, Object>>> fallback(@PathVariable String service) {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "error", "service_unavailable",
                "message", service + " is currently unavailable, please retry shortly",
                "timestamp", Instant.now().toString())));
    }
}
