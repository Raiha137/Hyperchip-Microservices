package com.hyperchip.master;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * HyperchipMasterApplication
 *
 * This is the main entry point for the Hyperchip Master Service.
 *
 * Purpose:
 * - Bootstraps the Spring Boot application
 * - Enables component scanning for all Hyperchip-related packages
 * - Registers this service with the service discovery server (Eureka)
 *
 * This service typically handles:
 * - Master data (brands, categories, products, offers, coupons, referrals)
 * - Central business logic shared across microservices
 */
@SpringBootApplication(
        scanBasePackages = "com.hyperchip"
)
@EnableDiscoveryClient
public class HyperchipMasterApplication {
    public static void main(String[] args) {
        SpringApplication.run(HyperchipMasterApplication.class, args);
    }
}
