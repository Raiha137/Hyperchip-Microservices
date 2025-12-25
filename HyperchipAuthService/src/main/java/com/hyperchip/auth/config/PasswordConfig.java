package com.hyperchip.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * PasswordConfig
 *
 * This configuration defines how passwords are encoded and verified
 * across the application.
 *
 * Developer notes (simple words):
 * - We use BCrypt for password hashing
 * - BCrypt is secure and recommended by Spring Security
 * - Same encoder is used for:
 *   - saving passwords during signup
 *   - checking passwords during login
 */
@Configuration
public class PasswordConfig {

    /**
     * PasswordEncoder bean used by Spring Security.
     *
     * - Encodes raw passwords before saving to DB
     * - Matches raw password with encoded password during login
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
