package com.paymentprocessing.payment_service.repository;

import com.paymentprocessing.payment_service.domain.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {
}
