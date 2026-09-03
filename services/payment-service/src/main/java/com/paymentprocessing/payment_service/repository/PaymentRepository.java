package com.paymentprocessing.payment_service.repository;

import com.paymentprocessing.payment_service.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByCustomerId(UUID customerId);
}
