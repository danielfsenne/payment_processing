package com.paymentprocessing.customer_service.web;

import com.paymentprocessing.customer_service.domain.Customer;
import com.paymentprocessing.customer_service.security.SecurityUtils;
import com.paymentprocessing.customer_service.service.CustomerService;
import com.paymentprocessing.customer_service.web.dto.CreateCustomerRequest;
import com.paymentprocessing.customer_service.web.dto.CustomerResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse create(@Valid @RequestBody CreateCustomerRequest request) {
        Customer customer = customerService.create(request);
        return CustomerResponse.from(customer);
    }

    @GetMapping("/{id}")
    public CustomerResponse getById(@PathVariable UUID id, Authentication authentication) {
        SecurityUtils.requireSelfOrAdmin(authentication, id);
        return CustomerResponse.from(customerService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<CustomerResponse> list() {
        return customerService.list().stream().map(CustomerResponse::from).toList();
    }
}
