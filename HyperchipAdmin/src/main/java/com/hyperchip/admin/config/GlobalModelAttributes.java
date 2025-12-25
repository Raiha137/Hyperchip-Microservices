package com.hyperchip.admin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    /**
     * Base URL of the Master Service.
     *
     * Purpose:
     * - This value is injected from application configuration
     * - Used globally across admin Thymeleaf views for building
     *   service-related links or API references
     */
    @Value("${master.service.url}")
    private String masterBaseUrl;

    /**
     * Adds the Master Service base URL as a global model attribute.
     *
     * Purpose:
     * - Automatically exposes `masterBaseUrl` to all controllers and views
     * - Eliminates the need to manually add this attribute in every controller
     * - Commonly used in Thymeleaf templates for dynamic URL construction
     */
    @ModelAttribute("masterBaseUrl")
    public String masterBaseUrl() {
        return masterBaseUrl;
    }

}
