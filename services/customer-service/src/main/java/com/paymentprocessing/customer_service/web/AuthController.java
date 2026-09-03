package com.paymentprocessing.customer_service.web;

import com.paymentprocessing.customer_service.domain.Customer;
import com.paymentprocessing.customer_service.security.TokenService;
import com.paymentprocessing.customer_service.service.CustomerService;
import com.paymentprocessing.customer_service.web.dto.LoginRequest;
import com.paymentprocessing.customer_service.web.dto.LoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CustomerService customerService;
    private final TokenService tokenService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        Customer customer = customerService.authenticate(request.email(), request.password());
        String token = tokenService.issue(customer);
        return LoginResponse.bearer(token, TokenService.TOKEN_TTL.toSeconds());
    }
}
