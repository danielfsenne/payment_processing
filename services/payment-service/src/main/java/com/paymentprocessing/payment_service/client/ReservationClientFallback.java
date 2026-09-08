package com.paymentprocessing.payment_service.client;

import com.paymentprocessing.payment_service.web.DownstreamUnavailableException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ReservationClientFallback implements ReservationClient {

    @Override
    public ReservationDto reserve(UUID accountId, CreateReservationRequest request) {
        throw new DownstreamUnavailableException("account-service");
    }

    @Override
    public ReservationDto confirm(UUID id) {
        throw new DownstreamUnavailableException("account-service");
    }

    @Override
    public ReservationDto release(UUID id) {
        throw new DownstreamUnavailableException("account-service");
    }
}
