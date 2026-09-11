package com.paymentprocessing.payment_service.repository;

import com.paymentprocessing.payment_service.domain.Payment;
import com.paymentprocessing.payment_service.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByCustomerId(UUID customerId);

    List<Payment> findByStatusInAndUpdatedAtBefore(Collection<PaymentStatus> statuses, Instant updatedBefore);
}
