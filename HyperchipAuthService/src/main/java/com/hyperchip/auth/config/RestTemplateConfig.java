package com.hyperchip.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplateConfig
 *
 * This class creates a RestTemplate bean.
 *
 * Developer notes (simple words):
 * - RestTemplate is used to call other services (microservices)
 * - Example: Auth service calling Wallet / User / Order service
 * - Spring will manage this object for the whole application
 */
@Configuration
public class RestTemplateConfig {

    /**
     * RestTemplate Bean
     *
     * - Used for HTTP calls (GET, POST, PUT, DELETE)
     * - Reused everywhere using @Autowired
     * - Avoids creating new RestTemplate objects again and again
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
