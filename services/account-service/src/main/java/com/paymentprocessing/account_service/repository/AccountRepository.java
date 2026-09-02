package com.paymentprocessing.account_service.repository;

import com.paymentprocessing.account_service.domain.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findByCustomerId(UUID customerId);

    /**
     * Reserving/confirming/releasing all follow a read-modify-write on balance and
     * reservedAmount. Under READ_COMMITTED, plain findById lets two concurrent
     * transactions read the same balance and both commit their write, over-reserving
     * the account. SELECT ... FOR UPDATE serializes those transactions on this row.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") UUID id);
}
