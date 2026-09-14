package com.paymentprocessing.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityUtilsTest {

    private Authentication authenticationFor(UUID customerId, String... roles) {
        List<SimpleGrantedAuthority> authorities = List.of(roles).stream().map(SimpleGrantedAuthority::new).toList();
        return new UsernamePasswordAuthenticationToken(customerId.toString(), "n/a", authorities);
    }

    @Test
    void isAdminIsTrueOnlyWithTheAdminRole() {
        UUID id = UUID.randomUUID();
        assertThat(SecurityUtils.isAdmin(authenticationFor(id, "ROLE_ADMIN"))).isTrue();
        assertThat(SecurityUtils.isAdmin(authenticationFor(id, "ROLE_CUSTOMER"))).isFalse();
    }

    @Test
    void currentCustomerIdParsesTheAuthenticationName() {
        UUID id = UUID.randomUUID();
        assertThat(SecurityUtils.currentCustomerId(authenticationFor(id, "ROLE_CUSTOMER"))).isEqualTo(id);
    }

    @Test
    void requireOwnerOrAdminAllowsTheResourceOwner() {
        UUID id = UUID.randomUUID();
        SecurityUtils.requireOwnerOrAdmin(authenticationFor(id, "ROLE_CUSTOMER"), id);
    }

    @Test
    void requireOwnerOrAdminAllowsAnAdminRegardlessOfOwnership() {
        SecurityUtils.requireOwnerOrAdmin(authenticationFor(UUID.randomUUID(), "ROLE_ADMIN"), UUID.randomUUID());
    }

    @Test
    void requireOwnerOrAdminRejectsAnUnrelatedCustomer() {
        UUID owner = UUID.randomUUID();
        Authentication someoneElse = authenticationFor(UUID.randomUUID(), "ROLE_CUSTOMER");

        assertThatThrownBy(() -> SecurityUtils.requireOwnerOrAdmin(someoneElse, owner))
                .isInstanceOf(AccessDeniedException.class);
    }
}
