package com.paymentprocessing.notification_worker.security;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

/**
 * Same distributed-authorization contract as the REST services, applied to STOMP frames:
 * SUBSCRIBE is only allowed onto the caller's own /topic/payments/{customerId} unless
 * the token carries ROLE_ADMIN. The caller's identity itself is established earlier, at
 * the WebSocket handshake (see JwtHandshakeInterceptor/JwtHandshakeHandler) - by the
 * time a STOMP frame reaches this interceptor, accessor.getUser() is already populated
 * or the connection was never allowed to upgrade. Throwing here causes Spring's STOMP
 * support to send the client an ERROR frame and close the session.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String TOPIC_PREFIX = "/topic/payments/";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }
        return message;
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith(TOPIC_PREFIX)) {
            throw new StompAuthorizationException("Unknown destination: " + destination);
        }
        if (!(accessor.getUser() instanceof JwtPrincipal principal)) {
            throw new StompAuthorizationException("Not authenticated");
        }

        String requestedCustomerId = destination.substring(TOPIC_PREFIX.length());
        if (!principal.isAdmin() && !principal.getName().equals(requestedCustomerId)) {
            throw new StompAuthorizationException(
                    "Not authorized to subscribe to payments for customer " + requestedCustomerId);
        }
    }
}
