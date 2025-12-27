// =========================
// HyperchipCartApplication.java
// =========================
package com.hyperchip.cart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * HyperchipCartApplication
 *
 * Main intention:
 * This is the entry point of the Hyperchip Cart microservice.
 *
 * Annotations:
 * @SpringBootApplication - marks this as a Spring Boot application
 * @EnableDiscoveryClient - registers the service with a discovery server (Eureka/Consul/etc.)
 *
 * The main method bootstraps the Spring context and starts the embedded Tomcat server.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class HyperchipCartApplication {

    /**
     * Main method to start the Cart microservice.
     *
     * @param args command-line arguments (optional)
     */
    public static void main(String[] args) {
        SpringApplication.run(HyperchipCartApplication.class, args);
    }
}
