package com.paymentprocessing.customer_service.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    public static UUID currentCustomerId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    public static void requireSelfOrAdmin(Authentication authentication, UUID targetCustomerId) {
        if (!isAdmin(authentication) && !currentCustomerId(authentication).equals(targetCustomerId)) {
            throw new AccessDeniedException("Not authorized to access this resource");
        }
    }
}
