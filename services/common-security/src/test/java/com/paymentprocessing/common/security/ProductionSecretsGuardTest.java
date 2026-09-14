package com.paymentprocessing.common.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionSecretsGuardTest {

    @Test
    void rejectsTheInsecureDevDefaultSecret() {
        assertThatThrownBy(() -> new ProductionSecretsGuard("dev-only-shared-secret-change-me-in-prod-32bytes"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void rejectsASecretShorterThan32Bytes() {
        assertThatThrownBy(() -> new ProductionSecretsGuard("too-short"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void acceptsAStrongUniqueSecret() {
        assertThatCode(() -> new ProductionSecretsGuard("a-sufficiently-long-and-unique-production-secret"))
                .doesNotThrowAnyException();
    }
}
