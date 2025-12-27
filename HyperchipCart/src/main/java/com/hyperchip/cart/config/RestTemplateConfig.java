package com.hyperchip.cart.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplateConfig
 *
 * This configuration class is responsible for providing
 * a RestTemplate bean for the application.
 *
 * RestTemplate is mainly used to make HTTP calls
 * to other microservices or external APIs.
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Creates a RestTemplate bean only if one
     * is not already available in the Spring context.
     *
     * Why ConditionalOnMissingBean?
     * - Prevents duplicate bean creation
     * - Allows overriding RestTemplate in future
     *   if customization is needed
     *
     * @return RestTemplate instance
     */
    @Bean
    @ConditionalOnMissingBean(RestTemplate.class)
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
