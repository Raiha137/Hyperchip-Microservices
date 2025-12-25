package com.hyperchip.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    /**
     * Creates and registers a RestTemplate bean in the Spring context.
     *
     * Purpose:
     * - Provides a centralized HTTP client for making REST calls
     *   to other microservices (Order Service, Master Service, etc.)
     * - Enables dependency injection of RestTemplate across the application
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
