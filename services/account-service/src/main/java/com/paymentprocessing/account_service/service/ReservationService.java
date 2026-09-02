package com.paymentprocessing.account_service.service;

import com.paymentprocessing.account_service.domain.Account;
import com.paymentprocessing.account_service.domain.Reservation;
import com.paymentprocessing.account_service.domain.ReservationStatus;
import com.paymentprocessing.account_service.repository.AccountRepository;
import com.paymentprocessing.account_service.repository.ReservationRepository;
import com.paymentprocessing.account_service.web.InsufficientFundsException;
import com.paymentprocessing.account_service.web.InvalidReservationStateException;
import com.paymentprocessing.account_service.web.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Backs the saga's "reserve balance" step and its compensations. Reserving only
 * moves money between balance and reservedAmount (both still belong to the
 * account), so a release always succeeds and always returns the account to exactly
 * the state it was in before the reservation - that's what makes it safe to call as
 * a compensating action.
 *
 * Every mutation is keyed by paymentId (reserve) or reservationId (confirm/release)
 * and is idempotent: retrying a saga step after a network blip must not double-debit
 * or double-release an account.
 */
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final AccountRepository accountRepository;
    private final ReservationRepository reservationRepository;

    @Transactional
    public Reservation reserve(UUID accountId, UUID paymentId, BigDecimal amount) {
        var existing = reservationRepository.findByPaymentId(paymentId);
        if (existing.isPresent()) {
            return existing.get();
        }

        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));

        if (amount.compareTo(account.getAvailableBalance()) > 0) {
            throw new InsufficientFundsException(accountId, amount, account.getAvailableBalance());
        }

        account.setReservedAmount(account.getReservedAmount().add(amount));
        accountRepository.save(account);

        Reservation reservation = Reservation.builder()
                .accountId(accountId)
                .paymentId(paymentId)
                .amount(amount)
                .status(ReservationStatus.RESERVED)
                .build();
        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation confirm(UUID reservationId) {
        Reservation reservation = getById(reservationId);
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            return reservation;
        }
        if (reservation.getStatus() != ReservationStatus.RESERVED) {
            throw new InvalidReservationStateException(reservationId, reservation.getStatus(), "confirm");
        }

        Account account = accountRepository.findByIdForUpdate(reservation.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + reservation.getAccountId()));
        account.setBalance(account.getBalance().subtract(reservation.getAmount()));
        account.setReservedAmount(account.getReservedAmount().subtract(reservation.getAmount()));
        accountRepository.save(account);

        reservation.setStatus(ReservationStatus.CONFIRMED);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation release(UUID reservationId) {
        Reservation reservation = getById(reservationId);
        if (reservation.getStatus() == ReservationStatus.RELEASED) {
            return reservation;
        }
        if (reservation.getStatus() != ReservationStatus.RESERVED) {
            throw new InvalidReservationStateException(reservationId, reservation.getStatus(), "release");
        }

        Account account = accountRepository.findByIdForUpdate(reservation.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + reservation.getAccountId()));
        account.setReservedAmount(account.getReservedAmount().subtract(reservation.getAmount()));
        accountRepository.save(account);

        reservation.setStatus(ReservationStatus.RELEASED);
        return reservationRepository.save(reservation);
    }

    @Transactional(readOnly = true)
    public Reservation getById(UUID id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));
    }
}
