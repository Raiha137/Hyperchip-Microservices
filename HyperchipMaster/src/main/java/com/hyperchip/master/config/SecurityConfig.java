package com.hyperchip.master.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfig
 *
 * Configures HTTP security for the Master service.
 *
 * Purpose:
 * - Disable CSRF protection (useful for stateless APIs or during development).
 * - Allow public access to static resources, API endpoints, and actuator endpoints.
 * - Disable frame options to allow embedding in iframes.
 */
@Configuration
public class SecurityConfig {

    /**
     * Configures the HTTP security filter chain.
     *
     * @param http HttpSecurity instance injected by Spring
     * @return SecurityFilterChain configured with rules
     * @throws Exception in case of configuration errors
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // -------------------------------
                // Disable CSRF protection
                // -------------------------------
                // Reason: API is stateless or during dev/testing. CSRF not needed for public APIs.
                .csrf(csrf -> csrf.disable())

                // -------------------------------
                // Configure authorization rules
                // -------------------------------
                .authorizeHttpRequests(auth -> auth
                        // Permit all requests to listed endpoints (public API, static files, actuator)
                        .requestMatchers(
                                "/**",                  // all endpoints
                                "/api/**",              // API endpoints
                                "/public/**",           // public resources
                                "/products/**",         // product pages
                                "/categories/**",       // category pages
                                "/brands/**",           // brand pages
                                "/uploads/**",          // uploaded files
                                "/img/**",              // images
                                "/css/**",              // CSS files
                                "/js/**",               // JS files
                                "/favicon.ico",         // favicon
                                "/actuator/**"          // Spring actuator endpoints
                        ).permitAll()
                        // Any other request also permitted (you can tighten this for production)
                        .anyRequest().permitAll()
                )

                // -------------------------------
                // Disable frame options
                // -------------------------------
                // Reason: allows embedding UI in iframes, useful for dashboards or H2 console
                .headers(headers -> headers
                        .frameOptions(frame -> frame.disable())
                );

        // Build and return the configured SecurityFilterChain
        return http.build();
    }
}
