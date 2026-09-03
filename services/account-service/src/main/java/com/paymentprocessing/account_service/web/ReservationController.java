package com.paymentprocessing.account_service.web;

import com.paymentprocessing.account_service.domain.Reservation;
import com.paymentprocessing.account_service.security.SecurityUtils;
import com.paymentprocessing.account_service.service.AccountService;
import com.paymentprocessing.account_service.service.ReservationService;
import com.paymentprocessing.account_service.web.dto.CreateReservationRequest;
import com.paymentprocessing.account_service.web.dto.ReservationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final AccountService accountService;

    @PostMapping("/accounts/{accountId}/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse reserve(@PathVariable UUID accountId, @Valid @RequestBody CreateReservationRequest request,
            Authentication authentication) {
        SecurityUtils.requireOwnerOrAdmin(authentication, accountService.getById(accountId).getCustomerId());
        var reservation = reservationService.reserve(accountId, request.paymentId(), request.amount());
        return ReservationResponse.from(reservation);
    }

    @GetMapping("/reservations/{id}")
    public ReservationResponse getById(@PathVariable UUID id, Authentication authentication) {
        Reservation reservation = reservationService.getById(id);
        requireOwnerOrAdminOfReservation(authentication, reservation);
        return ReservationResponse.from(reservation);
    }

    @PostMapping("/reservations/{id}/confirm")
    public ReservationResponse confirm(@PathVariable UUID id, Authentication authentication) {
        requireOwnerOrAdminOfReservation(authentication, reservationService.getById(id));
        return ReservationResponse.from(reservationService.confirm(id));
    }

    @PostMapping("/reservations/{id}/release")
    public ReservationResponse release(@PathVariable UUID id, Authentication authentication) {
        requireOwnerOrAdminOfReservation(authentication, reservationService.getById(id));
        return ReservationResponse.from(reservationService.release(id));
    }

    private void requireOwnerOrAdminOfReservation(Authentication authentication, Reservation reservation) {
        SecurityUtils.requireOwnerOrAdmin(authentication, accountService.getById(reservation.getAccountId()).getCustomerId());
    }
}
