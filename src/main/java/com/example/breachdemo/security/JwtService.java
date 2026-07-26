package com.example.breachdemo.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Issues and verifies signed JWTs.
 *
 * This is the "authentication" the v1 stage adds. Note what a JWT does and does
 * NOT do: a valid token proves *who* the caller is (the subject). It says nothing
 * about *what objects* that caller is allowed to access. That gap is exactly why
 * v1 still leaks every record — see CustomerControllerV1.
 */
@Service
public class JwtService {

    // Demo secret only. In a real system this would be externalised (env/secret
    // manager) and rotated, never committed to source control.
    private static final String SECRET =
            "demo-secret-key-for-comp6441-breach-demo-change-me-0123456789";
    private static final long EXPIRY_MS = 60 * 60 * 1000L; // 1 hour

    private final SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    public String issueToken(String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(EXPIRY_MS)))
                .signWith(key)
                .compact();
    }

    /**
     * Returns the username (subject) if the token is valid, otherwise throws a
     * JwtException. Callers treat any exception as "not authenticated".
     */
    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}
