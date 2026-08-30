package com.paymentprocessing.account_service.web;

import com.paymentprocessing.account_service.domain.Account;
import com.paymentprocessing.account_service.service.AccountService;
import com.paymentprocessing.account_service.web.dto.AccountResponse;
import com.paymentprocessing.account_service.web.dto.CreateAccountRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    public AccountResponse create(@Valid @RequestBody CreateAccountRequest request) {
        Account account = accountService.create(request);
        return AccountResponse.from(account);
    }

    @GetMapping("/{id}")
    public AccountResponse getById(@PathVariable UUID id) {
        return AccountResponse.from(accountService.getById(id));
    }

    @GetMapping
    public List<AccountResponse> list() {
        return accountService.list().stream().map(AccountResponse::from).toList();
    }
}
