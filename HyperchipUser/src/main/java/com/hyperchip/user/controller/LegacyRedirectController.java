package com.hyperchip.user.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * LegacyRedirectController
 * ------------------------
 * Handles redirects for old URL patterns to the new user-prefixed URLs.
 *
 * Purpose:
 * - Maintain backward compatibility for bookmarks or external links
 * - Redirects /products to /user/products
 * - Redirects /product/{id} to /user/product/{id}
 */
@Controller
public class LegacyRedirectController {

    /**
     * redirectProducts
     * ----------------
     * GET /products
     *
     * Purpose:
     * - Redirect anonymous /products requests to the new /user/products endpoint
     */
    @GetMapping("/products")
    public String redirectProducts() {
        return "redirect:/user/products";
    }

    /**
     * redirectProduct
     * ---------------
     * GET /product/{id}
     *
     * Purpose:
     * - Redirects /product/{id} requests to the new /user/product/{id} endpoint
     * - Preserves the product ID in the redirect
     */
    @GetMapping("/product/{id}")
    public String redirectProduct(@PathVariable("id") Long id) {
        return "redirect:/user/product/" + id;
    }
}