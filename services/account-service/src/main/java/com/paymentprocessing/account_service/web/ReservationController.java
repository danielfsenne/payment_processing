package com.paymentprocessing.account_service.web;

import com.paymentprocessing.account_service.service.ReservationService;
import com.paymentprocessing.account_service.web.dto.CreateReservationRequest;
import com.paymentprocessing.account_service.web.dto.ReservationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping("/accounts/{accountId}/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse reserve(@PathVariable UUID accountId, @Valid @RequestBody CreateReservationRequest request) {
        var reservation = reservationService.reserve(accountId, request.paymentId(), request.amount());
        return ReservationResponse.from(reservation);
    }

    @GetMapping("/reservations/{id}")
    public ReservationResponse getById(@PathVariable UUID id) {
        return ReservationResponse.from(reservationService.getById(id));
    }

    @PostMapping("/reservations/{id}/confirm")
    public ReservationResponse confirm(@PathVariable UUID id) {
        return ReservationResponse.from(reservationService.confirm(id));
    }

    @PostMapping("/reservations/{id}/release")
    public ReservationResponse release(@PathVariable UUID id) {
        return ReservationResponse.from(reservationService.release(id));
    }
}
