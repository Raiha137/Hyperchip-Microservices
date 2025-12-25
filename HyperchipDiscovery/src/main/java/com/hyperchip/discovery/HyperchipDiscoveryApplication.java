package com.hyperchip.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * ==============================================================
 * Hyperchip Discovery Service (Eureka Server)
 * --------------------------------------------------------------
 * This Spring Boot application acts as the Eureka Discovery Server
 * for the entire Hyperchip microservices ecosystem.
 *
 * Purpose:
 *  - Registers all running microservices
 *  - Allows services to discover each other dynamically
 *  - Removes the need to hard-code service IP addresses and ports
 *  - Improves load balancing and service resiliency
 *
 * @EnableEurekaServer:
 *  Enables Eureka Server features such as the dashboard,
 *  heartbeat monitoring, status checks, and service registry.
 *
 * This application should be started before other microservices
 * so they can register themselves during startup.
 * ==============================================================
 */
@SpringBootApplication
@EnableEurekaServer
public class HyperchipDiscoveryApplication {
    public static void main(String[] args) {
        SpringApplication.run(HyperchipDiscoveryApplication.class, args);
    }

}
