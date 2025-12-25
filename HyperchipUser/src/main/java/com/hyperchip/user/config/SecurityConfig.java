package com.hyperchip.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfig
 * ----------------
 * This configuration class is responsible for defining
 * Spring Security behavior for the User Service.
 *
 * Purpose:
 * - Controls which URLs are publicly accessible
 * - Disables CSRF for stateless / proxy-based usage
 * - Configures logout behavior
 */
@Configuration
public class SecurityConfig {

    /**
     * securityFilterChain
     * -------------------
     * This method defines the main Spring Security filter chain.
     *
     * Why this method is needed:
     * - Spring Security 6+ uses SecurityFilterChain instead of WebSecurityConfigurerAdapter
     * - It allows fine-grained control over request authorization rules
     *
     * What this configuration does:
     * - Disables CSRF protection (useful for APIs and gateway-based calls)
     * - Allows public access to static resources and public pages
     * - Configures logout behavior
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                /**
                 * CSRF Configuration
                 * ------------------
                 * CSRF protection is disabled because:
                 * - This service is mainly consumed via APIs / proxies
                 * - Tokens or gateway-level security is expected instead
                 */
                .csrf(csrf -> csrf.disable())

                /**
                 * Authorization Rules
                 * -------------------
                 * Defines which HTTP requests are allowed without authentication
                 */
                .authorizeHttpRequests(auth -> auth

                        // Allow actuator endpoints for monitoring and health checks
                        .requestMatchers("/actuator/**").permitAll()

                        // Allow static resources (CSS, JS, images, uploads, etc.)
                        .requestMatchers(
                                "/css/**",
                                "/js/**",
                                "/img/**",
                                "/media/**",
                                "/uploads/**"
                        ).permitAll()

                        // Allow public-facing pages and SSO completion endpoint
                        .requestMatchers(
                                "/sso/finish",
                                "/user/**",
                                "/",
                                "/products/**",
                                "/product/**"
                        ).permitAll()

                        // Allow API proxy endpoints used to communicate with other microservices
                        .requestMatchers("/user/api/proxy/**").permitAll()

                        // Specifically allow delivery-related proxy APIs
                        .requestMatchers("/user/api/proxy/delivery/**").permitAll()

                        // Allow all remaining requests (can be tightened later if needed)
                        .anyRequest().permitAll()
                )

                /**
                 * Logout Configuration
                 * --------------------
                 * Defines how logout should behave in the application
                 */
                .logout(logout -> logout

                        // URL that triggers logout
                        .logoutUrl("/logout")

                        // Redirect user to login page after successful logout
                        .logoutSuccessUrl("http://localhost:8084/login?logout")

                        // Invalidate HTTP session to clear user data
                        .invalidateHttpSession(true)

                        // Remove session cookie from browser
                        .deleteCookies("JSESSIONID")
                );

        /**
         * Builds and returns the configured SecurityFilterChain
         */
        return http.build();
    }
}
