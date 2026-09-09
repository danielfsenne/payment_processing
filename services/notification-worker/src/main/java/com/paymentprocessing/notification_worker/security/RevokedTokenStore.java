package com.paymentprocessing.notification_worker.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * notification-worker never issues or revokes a token (only customer-service does, on
 * logout), but it checks every WebSocket handshake against the same denylist so a
 * revoked token can't keep an existing live-update subscription authenticated either.
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
