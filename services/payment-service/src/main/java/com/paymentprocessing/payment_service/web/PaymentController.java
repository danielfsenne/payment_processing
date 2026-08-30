package com.paymentprocessing.payment_service.web;

import com.paymentprocessing.payment_service.domain.Payment;
import com.paymentprocessing.payment_service.service.PaymentService;
import com.paymentprocessing.payment_service.web.dto.CreatePaymentRequest;
import com.paymentprocessing.payment_service.web.dto.PaymentResponse;
import com.paymentprocessing.payment_service.web.dto.TransitionRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse create(@Valid @RequestBody CreatePaymentRequest request) {
        Payment payment = paymentService.create(request);
        return PaymentResponse.from(payment);
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
}
