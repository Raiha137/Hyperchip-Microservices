package com.hyperchip.order.config;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplateConfig
 *
 * Purpose:
 * - Provides a Spring-managed RestTemplate bean for making HTTP calls.
 * - Uses Apache HttpClient 5 (CloseableHttpClient) for better performance
 *   and advanced HTTP features (timeouts, connection pooling, etc.).
 *
 * Developer notes:
 * - All services (like OfferClient, PaymentServiceImpl) should inject this RestTemplate
 *   instead of creating their own instances.
 * - This configuration ensures that the RestTemplate uses HttpClient 5 under the hood.
 */
@Configuration
public class RestTemplateConfig {

    /**
     * RestTemplate bean
     *
     * Flow:
     * 1. Create an Apache CloseableHttpClient instance
     * 2. Wrap it in HttpComponentsClientHttpRequestFactory
     * 3. Provide it to RestTemplate so that RestTemplate uses Apache HttpClient
     *
     * Benefits:
     * - Better performance and connection management compared to default SimpleClientHttpRequestFactory
     * - Allows future enhancements like custom timeouts, interceptors, or retry logic
     *
     * @return a configured RestTemplate instance ready for dependency injection
     */
    @Bean
    public RestTemplate restTemplate() {
        // Step 1: Create a default CloseableHttpClient (HttpClient 5)
        CloseableHttpClient httpClient = HttpClients.createDefault();

        // Step 2: Wrap the HttpClient in Spring's request factory
        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        // Step 3: Return a RestTemplate using the custom request factory
        return new RestTemplate(requestFactory);
    }
}
