package com.paymentprocessing.notification_worker.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

@RequiredArgsConstructor
public class NotRevokedTokenValidator implements OAuth2TokenValidator<Jwt> {

    private final RevokedTokenStore revokedTokenStore;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        if (revokedTokenStore.isRevoked(token.getId())) {
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("token_revoked", "This token has been revoked", null));
        }
        return OAuth2TokenValidatorResult.success();
    }
}
