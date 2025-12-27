package com.hyperchip.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main entry point for the Hyperchip Order Service application.
 *
 * <p>This class bootstraps the Spring Boot application and enables
 * transaction management for service layer methods.</p>
 *
 * <p>The application scans the following base packages:</p>
 * <ul>
 *     <li>com.hyperchip.order</li>
 *     <li>com.hyperchip.common</li>
 * </ul>
 */
@SpringBootApplication(scanBasePackages = {
        "com.hyperchip.order",
        "com.hyperchip.common"
})
@EnableTransactionManagement
public class HyperchipOrderApplication {

    /**
     * Main method to start the Spring Boot application.
     *
     * @param args command-line arguments (optional)
     */
    public static void main(String[] args) {
        SpringApplication.run(HyperchipOrderApplication.class, args);
    }
}
