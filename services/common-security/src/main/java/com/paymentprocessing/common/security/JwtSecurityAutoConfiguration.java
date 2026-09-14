package com.paymentprocessing.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import javax.crypto.spec.SecretKeySpec;

/**
 * Every service in this system validates the same HS256 JWT that customer-service
 * issues, against the same shared secret, with the same "roles" claim -> ROLE_*
 * authority mapping, and the same revoked-token denylist. This auto-configuration is
 * that one definition, picked up automatically by any service that depends on
 * common-security (see META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports)
 * with no per-service wiring required.
 *
 * Every bean backs off (@ConditionalOnMissingBean) if the consuming service already
 * defines its own - customer-service does for JwtDecoder, since it derives the
 * decoder's key from the same SecretKeySpec bean it also uses to encode new tokens.
 */
@AutoConfiguration
public class JwtSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RevokedTokenStore.class)
    public RevokedTokenStore revokedTokenStore(StringRedisTemplate redisTemplate) {
        return new RevokedTokenStore(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    public JwtDecoder jwtDecoder(@Value("${security.jwt.secret}") String secret, RevokedTokenStore revokedTokenStore) {
        SecretKeySpec key = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(), new NotRevokedTokenValidator(revokedTokenStore));
        decoder.setJwtValidator(validator);
        return decoder;
    }

    @Bean
    @ConditionalOnMissingBean(JwtAuthenticationConverter.class)
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    @Bean
    @ConditionalOnMissingBean(ProductionSecretsGuard.class)
    @Profile("prod")
    public ProductionSecretsGuard productionSecretsGuard(@Value("${security.jwt.secret}") String jwtSecret) {
        return new ProductionSecretsGuard(jwtSecret);
    }
}
