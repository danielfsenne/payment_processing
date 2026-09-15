package com.paymentprocessing.payment_service.repository;

import com.paymentprocessing.payment_service.domain.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {

    @Modifying
    @Query("DELETE FROM IdempotencyKey k WHERE k.createdAt < :threshold")
    int deleteByCreatedAtBefore(@Param("threshold") Instant threshold);
}
