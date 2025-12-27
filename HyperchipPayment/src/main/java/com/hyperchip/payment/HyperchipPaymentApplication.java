package com.hyperchip.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

/**
 * Main entry point for the Hyperchip Payment Service.
 *
 * This class boots the Spring application and registers
 * this service with Eureka for service discovery.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class HyperchipPaymentApplication {

    /**
     * Application start method.
     * Starts the embedded server and initializes Spring context.
     */
    public static void main(String[] args) {
        SpringApplication.run(HyperchipPaymentApplication.class, args);
    }

    /**
     * RestTemplate bean.
     *
     * Used to communicate with other microservices
     * (for example: Order, Wallet, Product services).
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
