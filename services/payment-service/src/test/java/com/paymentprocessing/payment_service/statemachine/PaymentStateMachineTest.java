package com.paymentprocessing.payment_service.statemachine;

import com.paymentprocessing.payment_service.domain.Payment;
import com.paymentprocessing.payment_service.domain.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import static com.paymentprocessing.payment_service.domain.PaymentStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentStateMachineTest {

    private final PaymentStateMachine stateMachine = new PaymentStateMachine();

    @Test
    void allowsTheHappyPathThroughSettlement() {
        Payment payment = paymentAt(CREATED);

        stateMachine.transition(payment, PROCESSING);
        assertThat(payment.getStatus()).isEqualTo(PROCESSING);

        stateMachine.transition(payment, AUTHORIZED);
        assertThat(payment.getStatus()).isEqualTo(AUTHORIZED);

        stateMachine.transition(payment, CAPTURED);
        assertThat(payment.getStatus()).isEqualTo(CAPTURED);

        stateMachine.transition(payment, SETTLED);
        assertThat(payment.getStatus()).isEqualTo(SETTLED);
    }

    @Test
    void allowsFailureAndRetryLoopBackIntoProcessing() {
        Payment payment = paymentAt(PROCESSING);

        stateMachine.transition(payment, FAILED);
        assertThat(payment.getStatus()).isEqualTo(FAILED);

        stateMachine.transition(payment, RETRYING);
        assertThat(payment.getStatus()).isEqualTo(RETRYING);

        stateMachine.transition(payment, PROCESSING);
        assertThat(payment.getStatus()).isEqualTo(PROCESSING);
    }

    @Test
    void rejectsSettledGoingBackToProcessing() {
        Payment payment = paymentAt(SETTLED);

        assertThatThrownBy(() -> stateMachine.transition(payment, PROCESSING))
                .isInstanceOf(InvalidStateTransitionException.class);
        assertThat(payment.getStatus()).isEqualTo(SETTLED);
    }

    @Test
    void settledIsTerminalAndAcceptsNoFurtherTransitions() {
        for (PaymentStatus candidate : PaymentStatus.values()) {
            assertThat(stateMachine.canTransition(SETTLED, candidate)).isFalse();
        }
    }

    @Test
    void rejectsSkippingStatesDirectlyToCaptured() {
        Payment payment = paymentAt(CREATED);

        assertThatThrownBy(() -> stateMachine.transition(payment, CAPTURED))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @ParameterizedTest
    @EnumSource(PaymentStatus.class)
    void everyStateOnlyAllowsItsExplicitlyDefinedTransitions(PaymentStatus from) {
        Set<PaymentStatus> expectedAllowed = switch (from) {
            case CREATED -> EnumSet.of(PROCESSING);
            case PROCESSING -> EnumSet.of(AUTHORIZED, FAILED);
            case AUTHORIZED -> EnumSet.of(CAPTURED, FAILED);
            case CAPTURED -> EnumSet.of(SETTLED, FAILED);
            case SETTLED -> EnumSet.noneOf(PaymentStatus.class);
            case FAILED -> EnumSet.of(RETRYING);
            case RETRYING -> EnumSet.of(PROCESSING);
        };

        for (PaymentStatus to : PaymentStatus.values()) {
            boolean actual = stateMachine.canTransition(from, to);
            boolean expected = expectedAllowed.contains(to);
            assertThat(actual)
                    .as("%s -> %s".formatted(from, to))
                    .isEqualTo(expected);
        }
    }

    private Payment paymentAt(PaymentStatus status) {
        return Payment.builder()
                .id(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .amount(BigDecimal.TEN)
                .currency("BRL")
                .status(status)
                .build();
    }
}
