package com.example.breachdemo.web;

import com.example.breachdemo.domain.Customer;
import com.example.breachdemo.service.CustomerService;
import com.example.breachdemo.service.OwnershipAuthorizationService;
import com.example.breachdemo.web.dto.SecureCustomerResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * v2: THE SECURED API.
 *
 * Compared with v1, the only behavioural change is the authorization decision:
 * every lookup now verifies that the caller actually owns the record. Combined
 * with UUID identifiers and rate limiting (see SecurityConfig / RateLimitFilter),
 * the enumeration attack no longer works.
 */
@RestController
@RequestMapping("/v2/api/customers")
public class CustomerControllerV2 {

    private final CustomerService service;
    private final OwnershipAuthorizationService authorization;

    public CustomerControllerV2(CustomerService service,
                                OwnershipAuthorizationService authorization) {
        this.service = service;
        this.authorization = authorization;
    }

    /**
     * Returns the caller's own record. No id is accepted at all — you can only
     * ever ask for yourself, which is the safest possible shape for this API.
     */
    @GetMapping("/me")
    public ResponseEntity<SecureCustomerResponse> me(Authentication auth) {
        return service.findByOwner(auth.getName())
                .map(SecureCustomerResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Look up a record by its UUID, but only return it if the caller owns it.
     *
     * Two defences at work here:
     *   1. The id is a UUID, so it cannot be enumerated by counting.
     *   2. The ownership check means that even if an attacker somehow obtains a
     *      valid UUID (leaked in a log, referer, shared link, ...), they still
     *      cannot read a record that is not theirs.
     *
     * Crucially, "record does not exist" and "record exists but is not yours"
     * both return an identical 404. That uniform response denies an attacker the
     * ability to use status codes as an oracle for which records exist.
     */
    @GetMapping("/{publicId}")
    public ResponseEntity<SecureCustomerResponse> getByPublicId(@PathVariable String publicId,
                                                                Authentication auth) {
        UUID uuid;
        try {
            uuid = UUID.fromString(publicId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build(); // malformed id -> same 404
        }

        Customer record = service.findByPublicId(uuid).orElse(null);
        if (record == null || !authorization.isOwner(record, auth.getName())) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(SecureCustomerResponse.from(record));
    }
}
