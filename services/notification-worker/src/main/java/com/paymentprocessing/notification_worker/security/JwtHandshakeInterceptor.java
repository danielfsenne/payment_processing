package com.paymentprocessing.notification_worker.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * A browser's WebSocket API cannot set an Authorization header on the handshake
 * request, so the JWT travels as a "token" query parameter instead - same token
 * customer-service issued, just relocated. A missing or invalid token fails the
 * handshake itself (HTTP 403, connection never upgrades) rather than being deferred to
 * the first STOMP frame.
 */
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    public static final String JWT_ATTRIBUTE = "jwt";

    private final JwtDecoder jwtDecoder;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst("token");
        if (token == null) {
            return false;
        }
        try {
            Jwt jwt = jwtDecoder.decode(token);
            attributes.put(JWT_ATTRIBUTE, jwt);
            return true;
        } catch (JwtException ex) {
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Exception exception) {
    }
}
