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
        http
                .csrf().disable()
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(
                                "/login",
                                "/signup",
                                "/verify-otp",
                                "/forgot-password",
                                "/reset-password",
                                "/oauth2/**",
                                "/css/**", "/js/**", "/img/**", "/uploads/**",
                                "/actuator/**"
                        ).permitAll()
                        .anyExchange().permitAll() // or adjust to your needs
                )
                .formLogin().disable()
                .httpBasic().disable();
        return http.build();
    }
}
