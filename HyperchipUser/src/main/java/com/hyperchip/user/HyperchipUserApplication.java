package com.hyperchip.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Main entry point for the Hyperchip User microservice.
 *
 * Purpose:
 * - Bootstraps the Spring Boot application.
 * - Configures entity scanning and repository scanning explicitly to avoid
 *   "Not a managed type" errors.
 * - Enables service discovery and asynchronous processing support.
 */
@SpringBootApplication(scanBasePackages = "com.hyperchip.user") // Base package for component scanning
@EnableDiscoveryClient // Enables this service to register with a discovery server (Eureka, Consul, etc.)
@EnableAsync // Enables @Async for asynchronous method execution
@EntityScan(basePackages = "com.hyperchip.user.model") // Scan JPA entities in the specified package
@EnableJpaRepositories(basePackages = "com.hyperchip.user.repository") // Scan JPA repositories in the specified package
public class HyperchipUserApplication {

    /**
     * Main method – application entry point.s
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(HyperchipUserApplication.class, args);
    }
}
