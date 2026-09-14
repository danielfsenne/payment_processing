package com.paymentprocessing.common.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevokedTokenStoreTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RevokedTokenStore revokedTokenStore;

    @Test
    void revokeWritesTheJtiWithTheRemainingTtl() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        revokedTokenStore.revoke("jti-1", Duration.ofMinutes(5));

        verify(valueOperations).set(eq("revoked-jwt:jti-1"), eq("1"), eq(Duration.ofMinutes(5)));
    }

    @Test
    void revokeIsANoOpForAnAlreadyExpiredOrMissingToken() {
        revokedTokenStore.revoke(null, Duration.ofMinutes(5));
        revokedTokenStore.revoke("jti-1", Duration.ZERO);
        revokedTokenStore.revoke("jti-1", Duration.ofSeconds(-1));

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void isRevokedReflectsWhetherTheKeyExists() {
        when(redisTemplate.hasKey("revoked-jwt:jti-1")).thenReturn(true);
        when(redisTemplate.hasKey("revoked-jwt:jti-2")).thenReturn(false);

        assertThat(revokedTokenStore.isRevoked("jti-1")).isTrue();
        assertThat(revokedTokenStore.isRevoked("jti-2")).isFalse();
        assertThat(revokedTokenStore.isRevoked(null)).isFalse();
    }
}
