// -------------------------
// HyperchipWishlistApplication
// -------------------------
// Main Spring Boot application entry point for the Wishlist microservice.
//
// Developer Notes:
// - @SpringBootApplication enables component scanning for com.hyperchip.wishlist and com.hyperchip.common.
// - @EntityScan ensures JPA entities in com.hyperchip.wishlist.model are detected.
// - @EnableJpaRepositories enables Spring Data JPA repositories in com.hyperchip.wishlist.repository.
// - main() method starts the Spring Boot application.
package com.hyperchip.wishlist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.hyperchip.wishlist", "com.hyperchip.common"})
@EntityScan(basePackages = {"com.hyperchip.wishlist.model"})
@EnableJpaRepositories(basePackages = {"com.hyperchip.wishlist.repository"})
public class HyperchipWishlistApplication {

    // -------------------------
    // Application Entry Point
    // -------------------------
    public static void main(String[] args) {
        SpringApplication.run(HyperchipWishlistApplication.class, args);
    }
}
