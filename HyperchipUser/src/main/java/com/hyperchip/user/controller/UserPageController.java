package com.hyperchip.user.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * UserPageController
 * ------------------
 * Handles simple user-facing pages like the homepage and static pages.
 * These pages typically do not require any backend logic.
 */
@Controller
public class UserPageController {

    /**
     * Homepage endpoint
     * URL: "/"
     * Returns the main user homepage view.
     */
    @GetMapping("/")
    public String homePage() {
        return "user/index";   // Corresponds to templates/user/index.html
    }

    /**
     * About page endpoint
     * URL: "/about"
     * Returns the about page view if it exists.
     */
    @GetMapping("/about")
    public String aboutPage() {
        return "user/about";   // Corresponds to templates/user/about.html
    }
}
