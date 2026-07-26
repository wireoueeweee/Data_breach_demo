package com.example.breachdemo.web;

import com.example.breachdemo.repository.CustomerRepository;
import com.example.breachdemo.security.JwtService;
import com.example.breachdemo.web.dto.LoginRequest;
import com.example.breachdemo.web.dto.LoginResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Issues JWTs to seeded demo accounts.
 *
 * Every seeded customer owns an account named "user1", "user2", ... . For this
 * demo they all share the password "password" — password security is not what
 * this project studies, so it is deliberately trivial. What matters is that a
 * caller ends up holding a valid token for ONE ordinary user.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String DEMO_PASSWORD = "password";

    private final CustomerRepository customers;
    private final JwtService jwtService;

    public AuthController(CustomerRepository customers, JwtService jwtService) {
        this.customers = customers;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        boolean userExists = request.username() != null
                && customers.existsByOwnerUsername(request.username());
        boolean passwordOk = DEMO_PASSWORD.equals(request.password());

        if (!userExists || !passwordOk) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid credentials"));
        }
        return ResponseEntity.ok(new LoginResponse(jwtService.issueToken(request.username())));
    }
}
