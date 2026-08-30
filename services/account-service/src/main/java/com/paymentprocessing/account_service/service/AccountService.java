package com.paymentprocessing.account_service.service;

import com.paymentprocessing.account_service.domain.Account;
import com.paymentprocessing.account_service.repository.AccountRepository;
import com.paymentprocessing.account_service.web.ResourceNotFoundException;
import com.paymentprocessing.account_service.web.dto.CreateAccountRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional
    public Account create(CreateAccountRequest request) {
        Account account = Account.builder()
                .customerId(request.customerId())
                .balance(request.initialBalance() != null ? request.initialBalance() : BigDecimal.ZERO)
                .currency(request.currency())
                .build();
        return accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public Account getById(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Account> list() {
        return accountRepository.findAll();
    }
}
