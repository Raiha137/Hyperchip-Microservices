package com.hyperchip.user.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * StaticImageConfig
 * -----------------
 * This configuration class customizes how static image resources
 * are served in the User Service.
 *
 * Purpose:
 * - Expose uploaded images from the file system
 * - Provide fallback/default images from classpath resources
 * - Avoid writing custom controllers just to serve images
 */
@Configuration
public class StaticImageConfig implements WebMvcConfigurer {

    /**
     * uploadsPath
     * -----------
     * Holds the file system path where uploaded images are stored.
     *
     * How it works:
     * - Reads value from application.properties or application.yml
     * - Falls back to "uploads" if the property is not defined
     *
     * Example:
     * uploads.file.path=/var/hyperchip/uploads
     */
    @Value("${uploads.file.path:uploads}")
    private String uploadsPath;

    /**
     * addResourceHandlers
     * -------------------
     * Registers custom resource handlers for serving static files.
     *
     * Why this method is used:
     * - Spring MVC allows mapping URL paths directly to
     *   file system or classpath locations
     * - Improves performance by bypassing controllers
     *
     * What this method configures:
     * 1. /uploads/**  -> Files stored on the server file system
     * 2. /img/**      -> Default images bundled inside the application
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        /**
         * Uploaded Images Handler
         * -----------------------
         * Maps URLs starting with /uploads/** to a directory
         * on the server file system.
         *
         * Usage:
         * - Product images uploaded by admin
         * - User profile images
         *
         * Example URL:
         * http://localhost:8080/uploads/product-123.png
         */
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadsPath + "/");

        /**
         * Fallback / Default Images Handler
         * --------------------------------
         * Maps /img/** URLs to static resources inside the JAR.
         *
         * Usage:
         * - Default product image
         * - Placeholder images when upload is missing
         *
         * Expected location:
         * src/main/resources/static/img/default-product.png
         *
         * Example URL:
         * http://localhost:8080/img/default-product.png
         */
        registry.addResourceHandler("/img/**")
                .addResourceLocations("classpath:/static/img/");
    }
}
