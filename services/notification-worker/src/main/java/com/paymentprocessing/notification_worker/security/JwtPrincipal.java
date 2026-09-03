package com.paymentprocessing.notification_worker.security;

import org.springframework.security.oauth2.jwt.Jwt;

import java.security.Principal;
import java.util.List;

public record JwtPrincipal(Jwt jwt) implements Principal {

    @Override
    public String getName() {
        return jwt.getSubject();
    }

    public boolean isAdmin() {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && roles.contains("ADMIN");
    }
}
