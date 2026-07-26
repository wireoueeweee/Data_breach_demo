package com.example.breachdemo.web.dto;

import com.example.breachdemo.domain.Customer;

import java.time.LocalDate;
import java.util.UUID;

/**
 * v2 response. Note the difference from CustomerResponse: it exposes the
 * unguessable publicId (UUID) and NOT the sequential database id. Leaking the
 * sequential id would hand enumeration straight back to an attacker, undoing the
 * UUID mitigation.
 *
 * Returning the full record (including governmentId) is fine here because v2 only
 * ever returns the caller's OWN record — you are only seeing your own data.
 */
public record SecureCustomerResponse(
        UUID publicId,
        String fullName,
        String email,
        String phone,
        LocalDate dateOfBirth,
        String address,
        String governmentId
) {
    public static SecureCustomerResponse from(Customer c) {
        return new SecureCustomerResponse(
                c.getPublicId(),
                c.getFullName(),
                c.getEmail(),
                c.getPhone(),
                c.getDateOfBirth(),
                c.getAddress(),
                c.getGovernmentId()
        );
    }
}
