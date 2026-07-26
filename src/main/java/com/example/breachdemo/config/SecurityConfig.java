package com.example.breachdemo.config;

import com.example.breachdemo.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Once Spring Security is on the classpath, every endpoint is locked down by
 * default. We therefore define the security posture per stage explicitly, so the
 * different versions can run side by side.
 *
 * Chain ordering matters: the /v1/** chain is evaluated first; anything it does
 * not match falls through to the permissive default chain.
 */
@Configuration
public class SecurityConfig {

    /**
     * v1: AUTHENTICATION ONLY.
     *
     * Every request under /v1/** must present a valid JWT. That is the *only*
     * thing checked. There is deliberately NO object-level authorization here,
     * which is why an authenticated user can still enumerate everyone's records.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain v1FilterChain(HttpSecurity http,
                                             JwtAuthenticationFilter jwtFilter) throws Exception {
        http
                .securityMatcher("/v1/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Everything else: the vulnerable v0 API, the login endpoint, and the H2
     * console are all left open. (v0 being open is the whole point of v0.)
     */
    @Bean
    @Order(2)
    public SecurityFilterChain openFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.disable())) // for H2 console
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
