package com.paymentprocessing.customer_service.service;

import com.paymentprocessing.customer_service.domain.Customer;
import com.paymentprocessing.customer_service.repository.CustomerRepository;
import com.paymentprocessing.customer_service.web.ResourceNotFoundException;
import com.paymentprocessing.customer_service.web.dto.CreateCustomerRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public Customer create(CreateCustomerRequest request) {
        Customer customer = Customer.builder()
                .name(request.name())
                .email(request.email())
                .document(request.document())
                .build();
        return customerRepository.save(customer);
    }

    @Transactional(readOnly = true)
    public Customer getById(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Customer> list() {
        return customerRepository.findAll();
    }
}
