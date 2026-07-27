package com.example.breachdemo.config;

import com.example.breachdemo.security.JwtAuthenticationFilter;
import com.example.breachdemo.security.RateLimitFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
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
 * Chain ordering matters: the more specific (securityMatcher) chains are
 * evaluated first; anything they do not match falls through to the permissive
 * default chain at the end.
 */
@Configuration
public class SecurityConfig {

    /**
     * v2: THE REAL FIX.
     *
     * Authentication (JWT) PLUS:
     *   - object-level authorization (enforced in CustomerControllerV2)
     *   - UUID public identifiers instead of sequential ids
     *   - per-user rate limiting (RateLimitFilter, defence in depth)
     */
    @Bean
    @Order(1)
    public SecurityFilterChain v2FilterChain(HttpSecurity http,
                                             JwtAuthenticationFilter jwtFilter,
                                             RateLimitFilter rateLimitFilter) throws Exception {
        http
                .securityMatcher("/v2/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                // Rate limit runs AFTER the JWT filter so it can key on the
                // authenticated username set into the SecurityContext.
                .addFilterAfter(rateLimitFilter, JwtAuthenticationFilter.class);
        return http.build();
    }

    /**
     * v1: AUTHENTICATION ONLY.
     *
     * Every /v1/** request needs a valid JWT and nothing more. No object-level
     * authorization, which is why an authenticated user can still enumerate
     * everyone's records. This is the deliberate "half fix".
     */
    @Bean
    @Order(2)
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
    @Order(3)
    public SecurityFilterChain openFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.disable())) // for H2 console
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    /**
     * Spring Boot auto-registers any Filter bean into the global servlet chain,
     * which would make these filters run for EVERY request (including v0). We
     * disable that so each filter runs ONLY where a security chain adds it,
     * keeping the stages cleanly isolated.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> disableJwtAutoRegistration(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> disableRateLimitAutoRegistration(
            RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }
}