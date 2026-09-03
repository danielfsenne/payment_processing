package com.paymentprocessing.account_service.web;

import com.paymentprocessing.account_service.domain.Account;
import com.paymentprocessing.account_service.security.SecurityUtils;
import com.paymentprocessing.account_service.service.AccountService;
import com.paymentprocessing.account_service.web.dto.AccountResponse;
import com.paymentprocessing.account_service.web.dto.CreateAccountRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody CreateAccountRequest request, Authentication authentication) {
        SecurityUtils.requireOwnerOrAdmin(authentication, request.customerId());
        Account account = accountService.create(request);
        return AccountResponse.from(account);
    }

    @GetMapping("/{id}")
    public AccountResponse getById(@PathVariable UUID id, Authentication authentication) {
        Account account = accountService.getById(id);
        SecurityUtils.requireOwnerOrAdmin(authentication, account.getCustomerId());
        return AccountResponse.from(account);
    }

    @GetMapping
    public List<AccountResponse> list(Authentication authentication) {
        List<Account> accounts = SecurityUtils.isAdmin(authentication)
                ? accountService.list()
                : accountService.listForCustomer(SecurityUtils.currentCustomerId(authentication));
        return accounts.stream().map(AccountResponse::from).toList();
    }
}
