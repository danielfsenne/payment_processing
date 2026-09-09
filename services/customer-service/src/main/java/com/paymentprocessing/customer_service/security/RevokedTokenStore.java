package com.paymentprocessing.customer_service.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * A JWT is stateless by design - nothing stops it from being honored right up to its
 * exp claim, even if the customer just logged out. This is the denylist that closes
 * that gap: logout writes the token's jti here with a TTL equal to its remaining
 * lifetime, so Redis expires the entry by itself the moment the token would have
 * expired anyway - no cleanup job needed. Every service that validates this JWT
 * (including this one) checks it via NotRevokedTokenValidator on every request.
 */
@Component
@RequiredArgsConstructor
public class RevokedTokenStore {

    private static final String KEY_PREFIX = "revoked-jwt:";

    private final StringRedisTemplate redisTemplate;

    public void revoke(String jti, Duration remainingTtl) {
        if (jti == null || remainingTtl.isNegative() || remainingTtl.isZero()) {
            return;
        }
        redisTemplate.opsForValue().set(KEY_PREFIX + jti, "1", remainingTtl);
    }

    public boolean isRevoked(String jti) {
        return jti != null && Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + jti));
    }
}
