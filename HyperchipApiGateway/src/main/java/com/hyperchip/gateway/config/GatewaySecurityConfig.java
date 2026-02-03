package com.hyperchip.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;

@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(ex -> ex
                        .pathMatchers(
                                "/login",
                                "/signup",
                                "/verify-otp",
                                "/forgot-password",
                                "/reset-password",
                                "/auth/**",
                                "/oauth2/**",
                                "/css/**",
                                "/js/**",
                                "/img/**",
                                "/uploads/**",
                                "/actuator/**"
                        ).permitAll()
                        .anyExchange().permitAll()
                )
                .build();
    }
}
