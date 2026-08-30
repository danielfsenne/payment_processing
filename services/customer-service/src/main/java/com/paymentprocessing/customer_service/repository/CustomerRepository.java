package com.paymentprocessing.customer_service.repository;

import com.paymentprocessing.customer_service.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
}
