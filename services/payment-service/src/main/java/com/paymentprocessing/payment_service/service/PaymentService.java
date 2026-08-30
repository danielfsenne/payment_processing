package com.paymentprocessing.payment_service.service;

import com.paymentprocessing.payment_service.client.AccountClient;
import com.paymentprocessing.payment_service.client.CustomerClient;
import com.paymentprocessing.payment_service.domain.Payment;
import com.paymentprocessing.payment_service.domain.PaymentStatus;
import com.paymentprocessing.payment_service.repository.PaymentRepository;
import com.paymentprocessing.payment_service.statemachine.PaymentStateMachine;
import com.paymentprocessing.payment_service.web.ResourceNotFoundException;
import com.paymentprocessing.payment_service.web.dto.CreatePaymentRequest;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentStateMachine stateMachine;
    private final CustomerClient customerClient;
    private final AccountClient accountClient;

    @Transactional
    public Payment create(CreatePaymentRequest request) {
        requireCustomerExists(request.customerId());
        requireAccountExists(request.accountId());

        Payment payment = Payment.builder()
                .customerId(request.customerId())
                .accountId(request.accountId())
                .amount(request.amount())
                .currency(request.currency())
                .status(PaymentStatus.CREATED)
                .build();
        return paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public Payment getById(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Payment> list() {
        return paymentRepository.findAll();
    }

    @Transactional
    public Payment transition(UUID id, PaymentStatus to) {
        Payment payment = getById(id);
        stateMachine.transition(payment, to);
        return paymentRepository.save(payment);
    }

    private void requireCustomerExists(UUID customerId) {
        try {
            customerClient.getById(customerId);
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Customer not found: " + customerId);
        }
    }

    private void requireAccountExists(UUID accountId) {
        try {
            accountClient.getById(accountId);
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Account not found: " + accountId);
        }
    }
}
