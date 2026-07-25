package com.example.breachdemo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "customers")
public class Customer {

    /**
     * Sequential, auto-incrementing primary key.
     *
     * THIS IS THE CORE VULNERABILITY. The v0 API exposes this value directly in
     * the URL (GET /v0/api/customers/{id}). Because the IDs are predictable and
     * contiguous (1, 2, 3, ...), an attacker can simply count upwards to
     * enumerate the entire dataset. This is the "predictable object reference"
     * half of the Optus-style breach.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unguessable public identifier. NOT used by the v0 API.
     *
     * In the secured version (v2) the API will expose this UUID instead of the
     * sequential id, so enumeration becomes infeasible. Note: swapping to a UUID
     * is a *mitigation* (it hurts enumeration), NOT the real fix. The real fix is
     * object-level authorization, added in v2.
     */
    @Column(nullable = false, unique = true, updatable = false)
    private UUID publicId = UUID.randomUUID();

    private String fullName;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private String address;

    /**
     * Sensitive government-ID-style field. Mirrors the passport / driver-licence
     * numbers exposed in the 2022 Optus breach. FAKE DATA ONLY.
     */
    private String governmentId;

    /**
     * The account that "owns" this record. Unused in v0.
     * Needed for the object-level authorization (BOLA) fix in v1/v2 — the secured
     * API will check that the caller actually owns the record they request.
     */
    private String ownerUsername;

    protected Customer() {
        // required by JPA
    }

    public Customer(String fullName, String email, String phone, LocalDate dateOfBirth,
                    String address, String governmentId, String ownerUsername) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
        this.governmentId = governmentId;
        this.ownerUsername = ownerUsername;
    }

    public Long getId() {
        return id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getAddress() {
        return address;
    }

    public String getGovernmentId() {
        return governmentId;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }
}
