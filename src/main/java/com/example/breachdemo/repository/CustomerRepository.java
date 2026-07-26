package com.example.breachdemo.repository;

import com.example.breachdemo.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    boolean existsByOwnerUsername(String ownerUsername);

    Optional<Customer> findByOwnerUsername(String ownerUsername);

    Optional<Customer> findByPublicId(UUID publicId);
}