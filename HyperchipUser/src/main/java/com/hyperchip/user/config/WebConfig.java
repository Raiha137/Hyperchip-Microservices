package com.hyperchip.user.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * WebConfig
 * ---------
 * This configuration class handles web-related customizations
 * for the User Service, specifically for serving user-uploaded files.
 *
 * Purpose:
 * - Ensure required upload directories exist at application startup
 * - Expose user-uploaded files via HTTP URLs
 * - Avoid using controllers for static file serving
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * usersUploadDir
     * --------------
     * Configuration property that defines where user-uploaded files
     * are stored on the file system.
     *
     * How it works:
     * - Reads value from application properties
     * - Uses "uploads/users" as default if not configured
     *
     * Example:
     * upload.users.dir=/var/hyperchip/uploads/users
     */
    @Value("${upload.users.dir:uploads/users}")
    private String usersUploadDir;

    /**
     * usersPath
     * ---------
     * Holds the normalized absolute Path object for the
     * user uploads directory.
     *
     * Why this is needed:
     * - Ensures consistent path handling across OS (Windows/Linux)
     * - Avoids repeated path conversion logic
     */
    private Path usersPath;

    /**
     * init
     * ----
     * This method runs once after the Spring bean is created.
     *
     * Purpose:
     * - Resolve the upload directory into an absolute, normalized path
     * - Automatically create the directory if it does not exist
     *
     * Why @PostConstruct is used:
     * - Guarantees directory setup before handling any web requests
     * - Prevents runtime failures during file upload or access
     */
    @PostConstruct
    public void init() throws Exception {

        // Convert configured directory into absolute normalized path
        usersPath = Paths.get(usersUploadDir)
                .toAbsolutePath()
                .normalize();

        // Create directory structure if it does not already exist
        if (!Files.exists(usersPath)) {
            Files.createDirectories(usersPath);
        }
    }

    /**
     * addResourceHandlers
     * -------------------
     * Registers custom resource handlers for serving static content.
     *
     * Purpose:
     * - Maps HTTP URLs directly to file system locations
     * - Improves performance by bypassing controller logic
     *
     * What is configured here:
     * - /uploads/users/**  -> user upload directory on the server
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        /**
         * User Uploads Resource Mapping
         * -----------------------------
         * Exposes user-uploaded files through HTTP URLs.
         *
         * Example:
         * http://localhost:8080/uploads/users/profile-123.jpg
         *
         * Cache Period:
         * - 3600 seconds (1 hour)
         * - Reduces repeated disk reads for frequently accessed images
         */
        registry.addResourceHandler("/uploads/users/**")
                .addResourceLocations("file:" + usersPath.toAbsolutePath() + "/")
                .setCachePeriod(3600);
    }
}
