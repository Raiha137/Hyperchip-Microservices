package com.hyperchip.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * HyperchipAdminApplication
 *
 * Entry point for the Admin module of Hyperchip e-commerce platform.
 *
 * Key Features:
 * - @SpringBootApplication: marks this as a Spring Boot application.
 * - @EnableDiscoveryClient: enables service registration/discovery (for microservices with Eureka/Consul).
 */
@SpringBootApplication
@EnableDiscoveryClient
public class HyperchipAdminApplication {

    /**
     * Main method - application startup.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(HyperchipAdminApplication.class, args);
    }
}
