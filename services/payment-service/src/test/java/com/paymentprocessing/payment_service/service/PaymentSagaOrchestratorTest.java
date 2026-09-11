package com.paymentprocessing.payment_service.service;

import com.paymentprocessing.payment_service.client.CreateReservationRequest;
import com.paymentprocessing.payment_service.client.ReservationClient;
import com.paymentprocessing.payment_service.client.ReservationDto;
import com.paymentprocessing.payment_service.domain.Payment;
import com.paymentprocessing.payment_service.domain.PaymentStatus;
import com.paymentprocessing.payment_service.web.ConcurrentOperationException;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentSagaOrchestratorTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private ReservationClient reservationClient;

    @Mock
    private DistributedLockService lockService;

    @InjectMocks
    private PaymentSagaOrchestrator sagaOrchestrator;

    @Test
    void rejectsConcurrentProcessingOfTheSamePaymentInsteadOfDoubleReserving() {
        UUID paymentId = UUID.randomUUID();
        when(lockService.tryLock(eq("payment-saga:" + paymentId), any(Duration.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> sagaOrchestrator.process(paymentId, false))
                .isInstanceOf(ConcurrentOperationException.class);

        verify(paymentService, never()).getById(any());
        verify(reservationClient, never()).reserve(any(), any());
    }

    @Test
    void releasesTheLockEvenWhenTheSagaFailsPartway() {
        UUID paymentId = UUID.randomUUID();
        String token = "lock-token";
        when(lockService.tryLock(eq("payment-saga:" + paymentId), any(Duration.class)))
                .thenReturn(Optional.of(token));
        when(paymentService.getById(paymentId))
                .thenReturn(Payment.builder().id(paymentId).status(PaymentStatus.AUTHORIZED).build());

        assertThatThrownBy(() -> sagaOrchestrator.process(paymentId, false))
                .isInstanceOf(IllegalStateException.class);

        verify(lockService).unlock("payment-saga:" + paymentId, token);
    }

    @Test
    void reconcileResumesAPaymentStuckInProcessingAllTheWayToSettled() {
        UUID paymentId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        String token = "lock-token";
        Payment stuck = Payment.builder().id(paymentId).accountId(accountId)
                .amount(BigDecimal.TEN).status(PaymentStatus.PROCESSING).build();
        Payment authorized = Payment.builder().id(paymentId).accountId(accountId)
                .amount(BigDecimal.TEN).status(PaymentStatus.AUTHORIZED).reservationId(reservationId).build();
        Payment captured = Payment.builder().id(paymentId).accountId(accountId)
                .amount(BigDecimal.TEN).status(PaymentStatus.CAPTURED).reservationId(reservationId).build();

        when(lockService.tryLock(eq("payment-saga:" + paymentId), any(Duration.class))).thenReturn(Optional.of(token));
        when(paymentService.getById(paymentId)).thenReturn(stuck);
        when(reservationClient.reserve(eq(accountId), any(CreateReservationRequest.class)))
                .thenReturn(new ReservationDto(reservationId, accountId, paymentId, BigDecimal.TEN, "RESERVED"));
        when(paymentService.transition(paymentId, PaymentStatus.AUTHORIZED)).thenReturn(authorized);
        when(paymentService.transition(paymentId, PaymentStatus.CAPTURED)).thenReturn(captured);

        sagaOrchestrator.reconcile(paymentId);

        verify(reservationClient).reserve(eq(accountId), any(CreateReservationRequest.class));
        verify(paymentService).attachReservation(paymentId, reservationId);
        verify(reservationClient).confirm(reservationId);
        verify(paymentService).transition(paymentId, PaymentStatus.SETTLED);
        verify(lockService).unlock("payment-saga:" + paymentId, token);
    }

    @Test
    void reconcileResumesAPaymentStuckInAuthorizedByReplayingConfirm() {
        UUID paymentId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        String token = "lock-token";
        Payment stuck = Payment.builder().id(paymentId).status(PaymentStatus.AUTHORIZED).reservationId(reservationId).build();
        Payment captured = Payment.builder().id(paymentId).status(PaymentStatus.CAPTURED).reservationId(reservationId).build();

        when(lockService.tryLock(eq("payment-saga:" + paymentId), any(Duration.class))).thenReturn(Optional.of(token));
        when(paymentService.getById(paymentId)).thenReturn(stuck);
        when(paymentService.transition(paymentId, PaymentStatus.CAPTURED)).thenReturn(captured);

        sagaOrchestrator.reconcile(paymentId);

        verify(reservationClient, never()).reserve(any(), any());
        verify(reservationClient).confirm(reservationId);
        verify(paymentService).transition(paymentId, PaymentStatus.SETTLED);
    }

    @Test
    void reconcileResumesAPaymentStuckInCapturedBySettlingDirectly() {
        UUID paymentId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        String token = "lock-token";
        Payment stuck = Payment.builder().id(paymentId).status(PaymentStatus.CAPTURED).reservationId(reservationId).build();

        when(lockService.tryLock(eq("payment-saga:" + paymentId), any(Duration.class))).thenReturn(Optional.of(token));
        when(paymentService.getById(paymentId)).thenReturn(stuck);

        sagaOrchestrator.reconcile(paymentId);

        verify(reservationClient, never()).confirm(any());
        verify(paymentService).transition(paymentId, PaymentStatus.SETTLED);
    }

    @Test
    void reconcileMarksFailedWhenReserveNowFindsInsufficientFunds() {
        UUID paymentId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        String token = "lock-token";
        Payment stuck = Payment.builder().id(paymentId).accountId(accountId)
                .amount(BigDecimal.TEN).status(PaymentStatus.PROCESSING).build();

        when(lockService.tryLock(eq("payment-saga:" + paymentId), any(Duration.class))).thenReturn(Optional.of(token));
        when(paymentService.getById(paymentId)).thenReturn(stuck);
        when(reservationClient.reserve(eq(accountId), any(CreateReservationRequest.class)))
                .thenThrow(conflict());

        sagaOrchestrator.reconcile(paymentId);

        verify(paymentService).markFailed(paymentId, "Insufficient funds");
        verify(paymentService, never()).transition(paymentId, PaymentStatus.AUTHORIZED);
    }

    @Test
    void reconcileLeavesThePaymentAsIsWhenTheDownstreamCallStillFails() {
        UUID paymentId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        String token = "lock-token";
        Payment stuck = Payment.builder().id(paymentId).status(PaymentStatus.AUTHORIZED).reservationId(reservationId).build();

        when(lockService.tryLock(eq("payment-saga:" + paymentId), any(Duration.class))).thenReturn(Optional.of(token));
        when(paymentService.getById(paymentId)).thenReturn(stuck);
        Request request = Request.create(Request.HttpMethod.POST, "/reservations/" + reservationId + "/confirm",
                Map.of(), null, new RequestTemplate());
        when(reservationClient.confirm(reservationId))
                .thenThrow(new feign.RetryableException(503, "timeout", Request.HttpMethod.POST, (Long) null, request));

        sagaOrchestrator.reconcile(paymentId);

        verify(paymentService, never()).markFailed(any(), any());
        verify(paymentService, never()).transition(paymentId, PaymentStatus.CAPTURED);
    }

    @Test
    void reconcileSkipsWhenTheSagaLockIsHeldByAnInFlightRequest() {
        UUID paymentId = UUID.randomUUID();
        when(lockService.tryLock(eq("payment-saga:" + paymentId), any(Duration.class))).thenReturn(Optional.empty());

        sagaOrchestrator.reconcile(paymentId);

        verify(paymentService, never()).getById(any());
    }

    private FeignException.Conflict conflict() {
        Request request = Request.create(Request.HttpMethod.POST, "/accounts/x/reservations",
                Map.of(), null, new RequestTemplate());
        feign.Response response = feign.Response.builder()
                .status(409)
                .reason("Conflict")
                .request(request)
                .headers(Map.of())
                .build();
        return (FeignException.Conflict) FeignException.errorStatus("reserve", response);
    }
}
