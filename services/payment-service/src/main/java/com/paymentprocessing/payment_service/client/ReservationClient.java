package com.paymentprocessing.payment_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "account-service-reservations", url = "${account-service.base-url}")
public interface ReservationClient {

    @PostMapping("/accounts/{accountId}/reservations")
    ReservationDto reserve(@PathVariable UUID accountId, @RequestBody CreateReservationRequest request);

    @PostMapping("/reservations/{id}/confirm")
    ReservationDto confirm(@PathVariable UUID id);

    @PostMapping("/reservations/{id}/release")
    ReservationDto release(@PathVariable UUID id);
}
