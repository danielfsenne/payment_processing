package com.paymentprocessing.payment_service.statemachine;

import com.paymentprocessing.payment_service.domain.Payment;
import com.paymentprocessing.payment_service.domain.PaymentStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.paymentprocessing.payment_service.domain.PaymentStatus.*;

/**
 * Explicit transition table for payment states. Any transition not listed here is
 * rejected, including ones that "sound" plausible (e.g. SETTLED -> PROCESSING).
 */
@Component
public class PaymentStateMachine {

    private static final Map<PaymentStatus, Set<PaymentStatus>> TRANSITIONS = new EnumMap<>(PaymentStatus.class);

    static {
        TRANSITIONS.put(CREATED, EnumSet.of(PROCESSING));
        TRANSITIONS.put(PROCESSING, EnumSet.of(AUTHORIZED, FAILED));
        TRANSITIONS.put(AUTHORIZED, EnumSet.of(CAPTURED, FAILED));
        TRANSITIONS.put(CAPTURED, EnumSet.of(SETTLED, FAILED));
        TRANSITIONS.put(SETTLED, EnumSet.noneOf(PaymentStatus.class));
        TRANSITIONS.put(FAILED, EnumSet.of(RETRYING));
        TRANSITIONS.put(RETRYING, EnumSet.of(PROCESSING));
    }

    public boolean canTransition(PaymentStatus from, PaymentStatus to) {
        return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    public void transition(Payment payment, PaymentStatus to) {
        PaymentStatus from = payment.getStatus();
        if (!canTransition(from, to)) {
            throw new InvalidStateTransitionException(from, to);
        }
        payment.setStatus(to);
    }
}
