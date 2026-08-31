package com.paymentprocessing.payment_service.service;

import com.paymentprocessing.payment_service.domain.IdempotencyKey;
import com.paymentprocessing.payment_service.repository.IdempotencyKeyRepository;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Backs the Idempotency-Key contract: replaying the same key with the same payload
 * returns the original response verbatim instead of creating a duplicate resource;
 * replaying it with a different payload is rejected as a conflict.
 */
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    public String fingerprint(Object request) {
        byte[] json = objectMapper.writeValueAsBytes(request);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(json));
    }

    public Optional<IdempotencyKey> find(String key) {
        return idempotencyKeyRepository.findById(key);
    }

    @SneakyThrows
    public IdempotencyKey record(String key, String fingerprint, int status, Object responseBody) {
        IdempotencyKey record = IdempotencyKey.builder()
                .key(key)
                .requestFingerprint(fingerprint)
                .responseStatus(status)
                .responseBody(objectMapper.writeValueAsString(responseBody))
                .build();
        return idempotencyKeyRepository.save(record);
    }

    public boolean fingerprintMatches(IdempotencyKey existing, String fingerprint) {
        return existing.getRequestFingerprint().equals(fingerprint);
    }
}
