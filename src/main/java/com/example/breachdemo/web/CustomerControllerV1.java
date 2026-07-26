package com.example.breachdemo.web;

import com.example.breachdemo.service.CustomerService;
import com.example.breachdemo.web.dto.CustomerResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * v1: THE HALF FIX.
 *
 * Reaching these endpoints now requires a valid JWT (enforced by SecurityConfig).
 * So we always know exactly who is calling: {@code auth.getName()}.
 *
 * And yet the breach is NOT fixed. Look at getById: we have the caller's
 * identity, but we never compare it against the record's owner. Any authenticated
 * user can therefore pull ANY customer id — the same enumeration attack works,
 * just with a token attached. This is Broken Object Level Authorization (OWASP
 * API1): authentication answered "who are you?" but nobody asked "are you allowed
 * to see THIS object?".
 *
 * The fix belongs in v2: compare the record's ownerUsername to auth.getName().
 */
@RestController
@RequestMapping("/v1/api/customers")
public class CustomerControllerV1 {

    private final CustomerService service;

    public CustomerControllerV1(CustomerService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getById(@PathVariable Long id, Authentication auth) {
        // We know who is calling (auth.getName()) ... and we ignore it entirely.
        // No ownership check happens here. That single omission is the vulnerability.
        return service.findById(id)
                .map(CustomerResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/count")
    public Map<String, Long> count() {
        return Map.of("total", service.count());
    }
}
