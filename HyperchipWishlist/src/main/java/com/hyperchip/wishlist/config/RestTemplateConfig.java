package com.hyperchip.wishlist.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    // -------------------------
    // Provides a singleton RestTemplate bean for HTTP calls
    // -------------------------
    @Bean
    @ConditionalOnMissingBean(RestTemplate.class)
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
