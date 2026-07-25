package com.example.breachdemo.service;

import com.example.breachdemo.domain.Customer;
import com.example.breachdemo.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository repository;

    public CustomerService(CustomerRepository repository) {
        this.repository = repository;
    }

    public Optional<Customer> findById(Long id) {
        return repository.findById(id);
    }

    public long count() {
        return repository.count();
    }
}
