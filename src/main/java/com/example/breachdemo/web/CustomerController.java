package com.example.breachdemo.web;

import com.example.breachdemo.service.CustomerService;
import com.example.breachdemo.web.dto.CustomerResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * VULNERABLE (v0) API. Do not copy this into anything real.
 *
 * Two design flaws, both present in the 2022 Optus breach:
 *   1. No authentication  -> anyone can call these endpoints (OWASP API2: Broken Auth)
 *   2. Sequential IDs      -> records can be enumerated 1..N (enables OWASP API1: BOLA)
 *
 * Later stages will live under /v1/... (auth only) and /v2/... (auth + object
 * authorization + UUIDs + rate limiting + detection), so all three postures can
 * be demonstrated side by side against the same attack script.
 */
@RestController
@RequestMapping("/v0/api/customers")
public class CustomerController {

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    /**
     * Returns a single customer's full record by sequential id. No auth, no
     * ownership check. This is the endpoint the enumeration attack hammers.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(CustomerResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Leaks the total number of records. Real APIs sometimes expose counts like
     * this; here it conveniently tells the attacker the exact enumeration range.
     */
    @GetMapping("/count")
    public Map<String, Long> count() {
        return Map.of("total", service.count());
    }
}
