package com.hyperchip.master.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * WebConfig
 *
 * Purpose:
 * - Configure static file handling (categories, brands, products uploads)
 * - Ensure upload directories exist on startup
 * - Configure CORS for cross-origin API calls
 */
@Configuration
public class WebConfig implements WebMvcConfigurer, InitializingBean {

    // -------------------------------
    // Directory configuration properties
    // -------------------------------
    @Value("${upload.categories.dir:#{null}}")
    private String categoriesDirProp;   // Optional override for categories

    @Value("${upload.brands.dir:#{null}}")
    private String brandsDirProp;       // Optional override for brands

    @Value("${upload.products.dir:${upload.dir:uploads/products}}")
    private String productsDirProp;     // Default: uploads/products

    @Value("${upload.base.dir:#{null}}")
    private String uploadBaseDirProp;   // Optional base directory for uploads

    // -------------------------------
    // Resolved absolute Paths for each upload type
    // -------------------------------
    private Path categoriesPath;
    private Path brandsPath;
    private Path productsPath;

    /**
     * After properties are set (on startup), initialize directories.
     *
     * Purpose:
     * - Convert configured paths to absolute paths
     * - Create directories if they do not exist
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        Path base = null;

        // Determine base directory if specified
        if (uploadBaseDirProp != null && !uploadBaseDirProp.isBlank()) {
            base = Paths.get(uploadBaseDirProp).toAbsolutePath().normalize();
            if (!Files.exists(base)) Files.createDirectories(base);
        }

        // Categories directory resolution
        if (categoriesDirProp != null && !categoriesDirProp.isBlank()) {
            categoriesPath = Paths.get(categoriesDirProp).toAbsolutePath().normalize();
        } else if (base != null) {
            categoriesPath = base.resolve("categories").toAbsolutePath().normalize();
        } else {
            categoriesPath = Paths.get("uploads/categories").toAbsolutePath().normalize();
        }
        if (!Files.exists(categoriesPath)) Files.createDirectories(categoriesPath);

        // Brands directory resolution
        if (brandsDirProp != null && !brandsDirProp.isBlank()) {
            brandsPath = Paths.get(brandsDirProp).toAbsolutePath().normalize();
        } else if (base != null) {
            brandsPath = base.resolve("brands").toAbsolutePath().normalize();
        } else {
            brandsPath = Paths.get("uploads/brands").toAbsolutePath().normalize();
        }
        if (!Files.exists(brandsPath)) Files.createDirectories(brandsPath);

        // Products directory resolution
        productsPath = Paths.get(productsDirProp).toAbsolutePath().normalize();
        if (!Files.exists(productsPath)) Files.createDirectories(productsPath);
    }

    /**
     * Configure Resource Handlers for serving static files.
     *
     * Purpose:
     * - Map URL paths (e.g., /uploads/products/**) to physical directories
     * - Set cache period for static resources (3600 seconds = 1 hour)
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        registry.addResourceHandler("/uploads/categories/**")
                .addResourceLocations("file:" + categoriesPath.toAbsolutePath() + "/")
                .setCachePeriod(3600);

        registry.addResourceHandler("/uploads/brands/**")
                .addResourceLocations("file:" + brandsPath.toAbsolutePath() + "/")
                .setCachePeriod(3600);

        registry.addResourceHandler("/uploads/products/**")
                .addResourceLocations("file:" + productsPath.toAbsolutePath() + "/")
                .setCachePeriod(3600);
    }

    /**
     * Configure Cross-Origin Resource Sharing (CORS) rules.
     *
     * Purpose:
     * - Allow the front-end (e.g., running on localhost:8083) to call API endpoints
     * - Permit common HTTP methods and credentials
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")                     // Apply to all API endpoints
                .allowedOrigins("http://localhost:8083")   // Frontend origin
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")                       // Allow all headers
                .allowCredentials(true);                   // Allow cookies / auth
    }
}
