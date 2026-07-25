package com.example.breachdemo.web.dto;

import com.example.breachdemo.domain.Customer;

import java.time.LocalDate;
import java.util.UUID;

/**
 * What the v0 API returns for a customer.
 *
 * Note that this DTO exposes EVERYTHING, including the sensitive governmentId.
 * A "tidy" full response object like this is convenient for legitimate clients,
 * but in the vulnerable version it also hands an attacker a clean, complete,
 * ready-to-use dump of each victim's personal data. In v2 the response for
 * other users' records will be locked down.
 */
public record CustomerResponse(
        Long id,
        UUID publicId,
        String fullName,
        String email,
        String phone,
        LocalDate dateOfBirth,
        String address,
        String governmentId
) {
    public static CustomerResponse from(Customer c) {
        return new CustomerResponse(
                c.getId(),
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
