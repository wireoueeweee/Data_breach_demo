package com.example.breachdemo.repository;

import com.example.breachdemo.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    // findById(Long) and count() are inherited from JpaRepository.
    // That is all v0 needs. Ownership-based lookups will be added for v1/v2.
    boolean existsByOwnerUsername(String ownerUsername);
}
