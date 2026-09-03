package com.paymentprocessing.payment_service.web;

import com.paymentprocessing.payment_service.domain.Payment;
import com.paymentprocessing.payment_service.security.SecurityUtils;
import com.paymentprocessing.payment_service.service.PaymentCreationResult;
import com.paymentprocessing.payment_service.service.PaymentSagaOrchestrator;
import com.paymentprocessing.payment_service.service.PaymentService;
import com.paymentprocessing.payment_service.web.dto.CreatePaymentRequest;
import com.paymentprocessing.payment_service.web.dto.PaymentEventResponse;
import com.paymentprocessing.payment_service.web.dto.PaymentResponse;
import com.paymentprocessing.payment_service.web.dto.ProcessPaymentRequest;
import com.paymentprocessing.payment_service.web.dto.TransitionRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentSagaOrchestrator sagaOrchestrator;

    @PostMapping
    public ResponseEntity<String> create(
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication) {
        SecurityUtils.requireOwnerOrAdmin(authentication, request.customerId());
        PaymentCreationResult result = paymentService.create(request, idempotencyKey);
        return ResponseEntity.status(result.status())
                .contentType(MediaType.APPLICATION_JSON)
                .body(result.body());
    }

    @GetMapping("/{id}")
    public PaymentResponse getById(@PathVariable UUID id, Authentication authentication) {
        Payment payment = paymentService.getById(id);
        SecurityUtils.requireOwnerOrAdmin(authentication, payment.getCustomerId());
        return PaymentResponse.from(payment);
    }

    @GetMapping
    public List<PaymentResponse> list(Authentication authentication) {
        List<Payment> payments = SecurityUtils.isAdmin(authentication)
                ? paymentService.list()
                : paymentService.listForCustomer(SecurityUtils.currentCustomerId(authentication));
        return payments.stream().map(PaymentResponse::from).toList();
    }

    @PostMapping("/{id}/transitions")
    @PreAuthorize("hasRole('ADMIN')")
    public PaymentResponse transition(@PathVariable UUID id, @Valid @RequestBody TransitionRequest request) {
        Payment payment = paymentService.transition(id, request.to());
        return PaymentResponse.from(payment);
    }

    @GetMapping("/{id}/events")
    public List<PaymentEventResponse> getEvents(@PathVariable UUID id, Authentication authentication) {
        SecurityUtils.requireOwnerOrAdmin(authentication, paymentService.getById(id).getCustomerId());
        return paymentService.getEvents(id).stream().map(PaymentEventResponse::from).toList();
    }

    @PostMapping("/{id}/process")
    public PaymentResponse process(
            @PathVariable UUID id,
            @RequestBody(required = false) ProcessPaymentRequest request,
            Authentication authentication) {
        SecurityUtils.requireOwnerOrAdmin(authentication, paymentService.getById(id).getCustomerId());
        ProcessPaymentRequest effective = request != null ? request : ProcessPaymentRequest.defaultRequest();
        Payment payment = sagaOrchestrator.process(id, effective.shouldSimulateFailure());
        return PaymentResponse.from(payment);
    }
}
