package com.paymentprocessing.payment_service.repository;

import com.paymentprocessing.payment_service.domain.OutboxEvent;
import com.paymentprocessing.payment_service.domain.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
