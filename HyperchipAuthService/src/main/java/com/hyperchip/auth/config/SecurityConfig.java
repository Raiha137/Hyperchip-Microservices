package com.hyperchip.auth.config;

import com.hyperchip.auth.security.CustomAuthenticationFailureHandler;
import com.hyperchip.auth.security.OAuth2LoginSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import java.util.List;

/**
 * SecurityConfig
 *
 * Developer simple notes:
 * - This file controls LOGIN, LOGOUT and ACCESS rules
 * - Handles normal login (email + password)
 * - Handles Google OAuth2 login
 * - Decides which URLs are open and which are protected
 */
@Configuration
public class SecurityConfig {

    // Handles what happens AFTER successful login (normal + Google)
    private final OAuth2LoginSuccessHandler successHandler;

    // Handles what happens when login FAILS
    private final CustomAuthenticationFailureHandler failureHandler;

    public SecurityConfig(OAuth2LoginSuccessHandler successHandler,
                          CustomAuthenticationFailureHandler failureHandler) {
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
    }

    /**
     * SecurityFilterChain
     *
     * - Main Spring Security configuration
     * - Controls CSRF, login, logout, OAuth2
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // Disable CSRF for simplicity (common in microservices)
                .csrf(csrf -> csrf.disable())

                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/login",
                                "/signup",
                                "/verify-otp",
                                "/forgot-password",
                                "/reset-password",
                                "/oauth2/**",
                                "/css/**",
                                "/js/**",
                                "/img/**",
                                "/actuator/**"
                        ).permitAll()


                        // Allow everything for now
                        // Later you can change to: .authenticated()
                        .anyRequest().permitAll()
                )

                /**
                 * FORM LOGIN (Email + Password)
                 */
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/do-login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        .permitAll()
                )


                /**
                 * GOOGLE OAUTH2 LOGIN
                 */
                .oauth2Login(oauth -> oauth
                        .loginPage("/login")              // same login page
                        .successHandler(successHandler)   // after Google login
                        .failureHandler(failureHandler)   // if Google login fails
                )

                /**
                 * LOGOUT CONFIGURATION
                 */
                .logout(logout -> logout
                        .logoutRequestMatcher(
                                new AntPathRequestMatcher("/logout", "GET")
                        )
                        .logoutSuccessUrl("/login?logout") // redirect after logout
                        .invalidateHttpSession(true)       // clear session
                        .deleteCookies("JSESSIONID")       // remove cookie
                );

        return http.build();
    }

    /**
     * AuthenticationManager
     *
     * Developer simple notes:
     * - Connects UserDetailsService + PasswordEncoder
     * - Used during email/password login
     */
    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService uds,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(uds);
        provider.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(List.of(provider));
    }
}
