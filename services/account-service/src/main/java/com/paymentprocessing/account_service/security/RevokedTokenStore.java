package com.paymentprocessing.account_service.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * account-service never issues or revokes a token (only customer-service does, on
 * logout), but it validates every request against the same denylist so a revoked token
 * stops working here too, not just at the issuer.
 */
@Component
@RequiredArgsConstructor
public class RevokedTokenStore {

    private static final String KEY_PREFIX = "revoked-jwt:";

    private final StringRedisTemplate redisTemplate;

    public boolean isRevoked(String jti) {
        return jti != null && Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + jti));
    }
}
