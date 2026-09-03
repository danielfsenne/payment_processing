package com.paymentprocessing.notification_worker.security;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

/**
 * Turns the Jwt that JwtHandshakeInterceptor stashed in the handshake attributes into
 * the WebSocketSession's Principal - this is what accessor.getUser() returns on every
 * STOMP frame for the lifetime of the session, CONNECT included.
 */
@Component
public class JwtHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        Jwt jwt = (Jwt) attributes.get(JwtHandshakeInterceptor.JWT_ATTRIBUTE);
        return jwt != null ? new JwtPrincipal(jwt) : null;
    }
}
