package com.hyperchip.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplateConfig
 *
 * Purpose:
 * - Provides a RestTemplate bean for this Payment service.
 * - Used to call other microservices (Order service, Wallet service, etc.).
 *
 * Developer notes (simple words):
 * - RestTemplate is Spring’s HTTP client.
 * - Wherever RestTemplate is `@Autowired`, this bean will be used.
 * - One shared bean is better than creating RestTemplate again and again.
 *
 * IMPORTANT:
 * - This class is meant to be a Spring configuration class.
 * - It should be picked up during component scanning.
 *
 * NOTE FOR DEVS:
 * - If this bean is not created at runtime, check whether this class
 *   is registered as a configuration class.
 */
public class RestTemplateConfig {

    /**
     * Creates and exposes RestTemplate as a Spring Bean.
     *
     * Used for:
     * - Calling Order Service
     * - Calling other internal APIs
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
