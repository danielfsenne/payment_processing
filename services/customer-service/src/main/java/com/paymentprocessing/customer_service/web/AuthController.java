package com.paymentprocessing.customer_service.web;

import com.paymentprocessing.common.security.RevokedTokenStore;
import com.paymentprocessing.customer_service.domain.Customer;
import com.paymentprocessing.customer_service.security.RefreshTokenService;
import com.paymentprocessing.customer_service.security.TokenService;
import com.paymentprocessing.customer_service.service.CustomerService;
import com.paymentprocessing.customer_service.web.dto.LoginRequest;
import com.paymentprocessing.customer_service.web.dto.LoginResponse;
import com.paymentprocessing.customer_service.web.dto.RefreshRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CustomerService customerService;
    private final TokenService tokenService;
    private final RevokedTokenStore revokedTokenStore;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        Customer customer = customerService.authenticate(request.email(), request.password());
        return issueTokenPair(customer);
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@Valid @RequestBody RefreshRequest request) {
        UUID customerId = refreshTokenService.consume(request.refreshToken())
                .orElseThrow(InvalidRefreshTokenException::new);
        Customer customer = customerService.getById(customerId);
        return issueTokenPair(customer);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal Jwt jwt, @RequestBody(required = false) RefreshRequest request) {
        Duration remainingTtl = Duration.between(Instant.now(), jwt.getExpiresAt());
        revokedTokenStore.revoke(jwt.getId(), remainingTtl);
        if (request != null) {
            refreshTokenService.revoke(request.refreshToken());
        }
    }

    private LoginResponse issueTokenPair(Customer customer) {
        String accessToken = tokenService.issue(customer);
        String refreshToken = refreshTokenService.issue(customer.getId());
        return LoginResponse.bearer(accessToken, TokenService.TOKEN_TTL.toSeconds(), refreshToken);
    }
}
