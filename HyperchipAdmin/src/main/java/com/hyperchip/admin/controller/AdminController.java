package com.hyperchip.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
//@RequestMapping("/admin")
public class AdminController {

    /**
     * Handles GET requests for the admin home page.
     *
     * Purpose:
     * - Serves as the main landing page for admin users
     * - Typically loaded after successful authentication
     * - Renders the admin dashboard UI
     */
    @GetMapping("/home")
    public String adminHome() {
        return "admin/index";
    }

    /**
     * Handles POST requests for the admin home page.
     *
     * Purpose:
     * - Supports form submissions or redirects that post back to the home page
     * - Prevents HTTP 405 errors when forms use POST method
     * - Redirects back to the same admin dashboard view
     * @return admin home view
     */
    @PostMapping("/home")
    public String adminHomePost() {
        return "admin/index";
    }

    /**
     * Gateway health check endpoint.
     *
     * Purpose:
     * - Used by API Gateway or load balancer to verify service availability
     * - Can be used during deployment or monitoring checks
     * - Lightweight endpoint without business logic
     */
    @GetMapping("/ping")
    public String ping() {
        return "admin/index";
    }

}
