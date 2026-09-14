package com.paymentprocessing.common.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotRevokedTokenValidatorTest {

    @Mock
    private RevokedTokenStore revokedTokenStore;

    private Jwt jwtWithId(String jti) {
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .claim("jti", jti)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    @Test
    void succeedsWhenTheTokenIsNotRevoked() {
        when(revokedTokenStore.isRevoked("jti-1")).thenReturn(false);
        NotRevokedTokenValidator validator = new NotRevokedTokenValidator(revokedTokenStore);

        OAuth2TokenValidatorResult result = validator.validate(jwtWithId("jti-1"));

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void failsWithTokenRevokedErrorWhenTheTokenIsRevoked() {
        when(revokedTokenStore.isRevoked("jti-1")).thenReturn(true);
        NotRevokedTokenValidator validator = new NotRevokedTokenValidator(revokedTokenStore);

        OAuth2TokenValidatorResult result = validator.validate(jwtWithId("jti-1"));

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).anySatisfy(error -> assertThat(error.getErrorCode()).isEqualTo("token_revoked"));
    }
}
