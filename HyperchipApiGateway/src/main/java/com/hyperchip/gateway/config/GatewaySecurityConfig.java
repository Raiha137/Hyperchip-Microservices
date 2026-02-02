//package com.hyperchip.gateway.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.web.server.ServerHttpSecurity;
//import org.springframework.security.web.server.SecurityWebFilterChain;
//
//@Configuration
//public class GatewaySecurityConfig {
//
//    @Bean
//    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
//        http
//                .csrf(ServerHttpSecurity.CsrfSpec::disable)
//                .authorizeExchange(exchange -> exchange
//                        .pathMatchers(
//                                "/actuator/**",
//                                "/login",
//                                "/signup",
//                                "/forgot-password",
//                                "/reset-password",
//                                "/verify-otp",
//                                "/oauth2/**",
//                                "/css/**",
//                                "/js/**",
//                                "/img/**"
//                        ).permitAll()
//                        .anyExchange().permitAll()
//                )
//                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
//                .formLogin(ServerHttpSecurity.FormLoginSpec::disable);
//
//        return http.build();
//    }
//}
