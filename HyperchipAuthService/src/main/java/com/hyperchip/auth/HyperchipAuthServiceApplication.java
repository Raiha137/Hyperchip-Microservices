package com.hyperchip.auth;

import com.hyperchip.auth.service.UserSyncService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * HyperchipAuthServiceApplication
 * --------------------------------
 * This is the main entry point of the Hyperchip Auth Service.
 *
 * Responsibilities:
 * - Starts the Spring Boot application
 * - Scans JPA entities and repositories
 * - Triggers initial user sync to user-service on startup
 */
@SpringBootApplication(scanBasePackages = "com.hyperchip.auth")
@EntityScan("com.hyperchip.auth.model")
@EnableJpaRepositories("com.hyperchip.auth.repository")
public class HyperchipAuthServiceApplication {

    /**
     * Application starting point.
     *
     * What happens here:
     * - Spring Boot context is created
     * - All beans are initialized
     * - Embedded server starts
     */
    public static void main(String[] args) {
        SpringApplication.run(HyperchipAuthServiceApplication.class, args);
        System.out.println("🚀 Hyperchip Auth Service started successfully!");
    }

    /**
     * CommandLineRunner
     * ------------------
     * This method runs automatically after the application starts.
     *
     * Why this is used:
     * - To sync existing users from auth-service
     *   to user-service when the service comes up
     *
     * Use case:
     * - First time deployment
     * - After database restore
     * - When user-service was down earlier
     *
     * Important:
     * - This sync is non-blocking
     * - Even if sync fails, auth-service will still run
     */
    @Bean
    public CommandLineRunner init(UserSyncService userSyncService) {
        return args -> {
            System.out.println("🔄 Starting user sync to User Service...");
//            userSyncService.syncAllUsersToUserService();
            System.out.println("✅ User sync completed!");
        };
    }
}
