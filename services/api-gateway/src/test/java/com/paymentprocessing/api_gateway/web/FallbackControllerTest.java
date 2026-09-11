package com.paymentprocessing.api_gateway.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackControllerTest {

    private final FallbackController controller = new FallbackController();

    @Test
    void respondsWithServiceUnavailableAndNamesTheDownstreamService() {
        ResponseEntity<Map<String, Object>> response = controller.fallback("payment-service").block();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        Map<String, Object> body = response.getBody();
        assertThat(body.get("error")).isEqualTo("service_unavailable");
        assertThat((String) body.get("message")).contains("payment-service");
        assertThat(body).containsKey("timestamp");
    }
}
