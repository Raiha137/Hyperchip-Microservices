package com.hyperchip.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class SecurityConfig {

    /**
     * Configures Spring Security filter chain for the Admin Service.
     *
     * Purpose:
     * - Defines which endpoints are accessible without authentication
     * - Disables CSRF protection for simplicity in server-to-server calls
     * - Configures logout behavior and session cleanup
     *
     * Security Scope:
     * - This service currently acts as a UI gateway / admin-facing service
     * - Authentication is handled externally (Auth Service)
     *
     * @param http HttpSecurity configuration object
     * @return configured SecurityFilterChain
     * @throws Exception in case of security configuration errors
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                /**
                 * CSRF protection is disabled.
                 *
                 * Reason:
                 * - Admin service consumes APIs from other microservices
                 * - CSRF is not required for server-side rendered admin views
                 * - Simplifies integration with external Auth Service
                 */
                .csrf(csrf -> csrf.disable())

                /**
                 * Authorization rules for incoming HTTP requests.
                 *
                 * All routes are currently permitted because:
                 * - Authentication is handled by a separate Auth Service
                 * - This service focuses on UI rendering and API aggregation
                 */
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()   // Health & monitoring
                        .requestMatchers("/api/admin/**").permitAll()  // Internal admin APIs
                        .requestMatchers("/uploads/**").permitAll()    // Static uploaded files
                        .requestMatchers("/admin/**").permitAll()      // Admin UI routes
                        .anyRequest().permitAll()                      // Fallback rule
                )

                /**
                 * Logout configuration.
                 *
                 * Purpose:
                 * - Clears local session data
                 * - Redirects user back to centralized Auth Service login page
                 * - Ensures proper session invalidation
                 */
                .logout(logout -> logout
                        .logoutRequestMatcher(
                                new AntPathRequestMatcher("/logout", "GET")
                        )
                        .logoutSuccessUrl(
                                "/login?logout"
                        ) // Redirect to Auth Service login page
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                );

        return http.build();
    }

}
