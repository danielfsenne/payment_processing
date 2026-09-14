package com.paymentprocessing.customer_service.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Opaque, single-use refresh tokens backed by Redis: "refresh-token:<token>" ->
 * customerId, with a TTL far longer than the access token's (TokenService.TOKEN_TTL)
 * so a client can silently mint a new access token instead of forcing the user to
 * log in again every hour. Each refresh consumes (deletes) the token and issues a
 * new one - rotation means a stolen-and-reused refresh token is only usable once
 * before both the thief's and the legitimate client's copies stop working, which
 * surfaces the compromise instead of leaving a long-lived credential replayable
 * indefinitely.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String KEY_PREFIX = "refresh-token:";
    public static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;

    public String issue(UUID customerId) {
        String token = UUID.randomUUID().toString() + UUID.randomUUID();
        redisTemplate.opsForValue().set(KEY_PREFIX + token, customerId.toString(), REFRESH_TOKEN_TTL);
        return token;
    }

    public Optional<UUID> consume(String token) {
        String key = KEY_PREFIX + token;
        String customerId = redisTemplate.opsForValue().get(key);
        if (customerId == null) {
            return Optional.empty();
        }
        redisTemplate.delete(key);
        return Optional.of(UUID.fromString(customerId));
    }

    public void revoke(String token) {
        if (token != null) {
            redisTemplate.delete(KEY_PREFIX + token);
        }
    }
}
