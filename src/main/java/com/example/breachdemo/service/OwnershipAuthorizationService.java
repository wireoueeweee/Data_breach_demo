package com.example.breachdemo.service;

import com.example.breachdemo.domain.Customer;
import org.springframework.stereotype.Service;

/**
 * The object-level authorization check that v1 was missing.
 *
 * This is the actual fix for the Optus-style breach. Authentication told us WHO
 * the caller is; this answers whether that caller is allowed to access a SPECIFIC
 * record. Kept as an explicit, standalone method (rather than a @PreAuthorize
 * annotation) purely so the check is visible and easy to demonstrate — in
 * production, Spring method security would be the idiomatic home for this.
 */
@Service
public class OwnershipAuthorizationService {

    public boolean isOwner(Customer record, String username) {
        return record != null
                && username != null
                && username.equals(record.getOwnerUsername());
    }
}
