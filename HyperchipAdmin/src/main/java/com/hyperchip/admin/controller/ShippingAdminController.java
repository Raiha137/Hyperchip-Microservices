package com.hyperchip.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * ShippingAdminController
 *
 * Purpose:
 * Handles the admin-side pages for managing shipping configurations.
 * Currently, it displays static shipping info and placeholder pages for adding zones.
 *
 * Future scope:
 * - Fetch shipping rules/zones from the database
 * - Add/edit/delete shipping zones
 */
@Controller
@RequestMapping("/admin/shipping")
public class ShippingAdminController {

    // ===================================================
    // SHIPPING CONFIGURATION PAGE
    // URL: GET /admin/shipping
    // View: templates/admin/shipping-config.html
    // ===================================================
    /**
     * Purpose:
     * Displays the admin shipping configuration page.
     * Currently shows static info about shipping rules.
     * Later can be updated to show dynamic shipping zones/rates.
     */
    @GetMapping
    public String shippingConfigPage(Model model) {
        // Pass static or dynamic data to the model if needed in the future
        return "admin/shipping-config"; // Thymeleaf template
    }

    // ===================================================
    // ADD SHIPPING ZONE PAGE (FUTURE)
    // URL: GET /admin/shipping/add
    // View: templates/admin/shipping-add.html
    // ===================================================
    /**
     * Purpose:
     * Shows a form for adding a new shipping zone.
     * Currently a placeholder.
     */
    @GetMapping("/add")
    public String addShippingZonePage(Model model) {
        // Future: Pass default values or dropdowns for zone creation
        return "admin/shipping-add"; // Thymeleaf template
    }
}
