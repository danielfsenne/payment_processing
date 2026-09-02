package com.paymentprocessing.payment_service.service;

import com.paymentprocessing.payment_service.client.AccountClient;
import com.paymentprocessing.payment_service.client.CustomerClient;
import com.paymentprocessing.payment_service.web.ResourceNotFoundException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Caches the "does this customer/account exist" check that payment creation performs
 * against customer-service and account-service on every request. These are read-mostly
 * reference lookups, so a short-TTL cache trades a small staleness window (a customer or
 * account removed in the last few seconds) for far fewer cross-service calls under load.
 * A NotFound result is never cached: Spring's cache abstraction only caches normal
 * returns, so a 404 always re-checks against the source service on the next call.
 */
@Service
@RequiredArgsConstructor
public class LookupCacheService {

    private final CustomerClient customerClient;
    private final AccountClient accountClient;

    @Cacheable(cacheNames = "customer-exists", key = "#customerId")
    public boolean customerExists(UUID customerId) {
        try {
            customerClient.getById(customerId);
            return true;
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Customer not found: " + customerId);
        }
    }

    @Cacheable(cacheNames = "account-exists", key = "#accountId")
    public boolean accountExists(UUID accountId) {
        try {
            accountClient.getById(accountId);
            return true;
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Account not found: " + accountId);
        }
    }
}
