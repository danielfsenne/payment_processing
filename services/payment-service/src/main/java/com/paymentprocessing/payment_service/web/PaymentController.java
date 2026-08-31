package com.paymentprocessing.payment_service.web;

import com.paymentprocessing.payment_service.domain.Payment;
import com.paymentprocessing.payment_service.service.PaymentCreationResult;
import com.paymentprocessing.payment_service.service.PaymentService;
import com.paymentprocessing.payment_service.web.dto.CreatePaymentRequest;
import com.paymentprocessing.payment_service.web.dto.PaymentEventResponse;
import com.paymentprocessing.payment_service.web.dto.PaymentResponse;
import com.paymentprocessing.payment_service.web.dto.TransitionRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<String> create(
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        PaymentCreationResult result = paymentService.create(request, idempotencyKey);
        return ResponseEntity.status(result.status())
                .contentType(MediaType.APPLICATION_JSON)
                .body(result.body());
    }

    @GetMapping("/{id}")
    public PaymentResponse getById(@PathVariable UUID id) {
        return PaymentResponse.from(paymentService.getById(id));
    }

    @GetMapping
    public List<PaymentResponse> list() {
        return paymentService.list().stream().map(PaymentResponse::from).toList();
    }

    @PostMapping("/{id}/transitions")
    public PaymentResponse transition(@PathVariable UUID id, @Valid @RequestBody TransitionRequest request) {
        Payment payment = paymentService.transition(id, request.to());
        return PaymentResponse.from(payment);
    }

    @GetMapping("/{id}/events")
    public List<PaymentEventResponse> getEvents(@PathVariable UUID id) {
        return paymentService.getEvents(id).stream().map(PaymentEventResponse::from).toList();
    }
}
