package com.paymentprocessing.account_service.repository;

import com.paymentprocessing.account_service.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    Optional<Reservation> findByPaymentId(UUID paymentId);
}
