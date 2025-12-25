package com.hyperchip.admin.controller;

import com.hyperchip.common.dto.ProductOfferDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * ProductOfferAdminController
 *
 * Purpose:
 * Handles Admin-side management of Product Offers.
 * - List all product offers
 * - Add or edit a product offer
 * - Delete product offers
 */
@Controller
@RequestMapping("/admin/offers/product")
@Slf4j
public class ProductOfferAdminController {

    @Value("${api.base.url}")
    private String API_BASE; // Base URL for the Admin API, e.g., http://localhost:8086/api/admin

    private final RestTemplate restTemplate = new RestTemplate();

    // ===================================================
    // LIST PRODUCT OFFERS
    // ===================================================
    /**
     * Purpose:
     * Display all product offers for admin.
     *
     * What it does:
     * - Calls API to fetch all product offers
     * - Converts array to List for Thymeleaf template
     * - Handles error if API call fails
     */
    @GetMapping
    public String listProductOffers(Model model) {
        try {
            String url = API_BASE + "/offers/product";
            ProductOfferDto[] offers = restTemplate.getForObject(url, ProductOfferDto[].class);

            // ✅ Convert array to List for easier iteration in Thymeleaf
            model.addAttribute("offers",
                    offers != null ? java.util.Arrays.asList(offers) : java.util.Collections.emptyList());
        } catch (Exception ex) {
            log.error("Failed to load product offers", ex);
            model.addAttribute("offers", java.util.Collections.emptyList());
            model.addAttribute("errorMsg", "Failed to load product offers.");
        }
        return "admin/product-offer-list"; // Thymeleaf view
    }

    // ===================================================
    // SHOW CREATE FORM
    // ===================================================
    /**
     * Purpose:
     * Display the form to create a new product offer.
     *
     * What it does:
     * - Adds empty ProductOfferDto for form binding
     */
    @GetMapping("/add")
    public String showCreateForm(Model model) {
        model.addAttribute("offer", new ProductOfferDto());
        return "admin/product-offer-form"; // Thymeleaf form view
    }

    // ===================================================
    // SHOW EDIT FORM
    // ===================================================
    /**
     * Purpose:
     * Display the form to edit an existing product offer.
     *
     * What it does:
     * - Fetches the product offer by ID from API
     * - Redirects back with error if not found
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id,
                               Model model,
                               RedirectAttributes ra) {
        try {
            String url = API_BASE + "/offers/product/" + id;
            ProductOfferDto dto = restTemplate.getForObject(url, ProductOfferDto.class);

            if (dto == null) {
                ra.addFlashAttribute("errorMsg", "Product offer not found.");
                return "redirect:/admin/offers/product";
            }

            model.addAttribute("offer", dto);
            return "admin/product-offer-form";

        } catch (Exception ex) {
            log.error("Failed to load product offer {}", id, ex);
            ra.addFlashAttribute("errorMsg", "Failed to load product offer.");
            return "redirect:/admin/offers/product";
        }
    }

    // ===================================================
    // SAVE OFFER (CREATE OR UPDATE)
    // ===================================================
    /**
     * Purpose:
     * Save a new product offer or update an existing one.
     *
     * What it does:
     * - Checks if offer ID is null → create, else update
     * - Calls appropriate API endpoint
     * - Adds success or error flash messages
     */
    @PostMapping("/save")
    public String saveProductOffer(@ModelAttribute("offer") ProductOfferDto offer,
                                   RedirectAttributes ra) {
        try {
            String base = API_BASE + "/offers/product";

            if (offer.getId() == null) {
                // Create new offer
                restTemplate.postForObject(base, offer, ProductOfferDto.class);
                ra.addFlashAttribute("successMsg", "Product offer created successfully.");
            } else {
                // Update existing offer
                String url = base + "/" + offer.getId();
                restTemplate.put(url, offer);
                ra.addFlashAttribute("successMsg", "Product offer updated successfully.");
            }

        } catch (Exception ex) {
            log.error("Failed to save product offer", ex);
            ra.addFlashAttribute("errorMsg", "Failed to save product offer.");
        }

        return "redirect:/admin/offers/product";
    }

    // ===================================================
    // DELETE OFFER
    // ===================================================
    /**
     * Purpose:
     * Delete a product offer by its ID.
     *
     * What it does:
     * - Calls API to delete the product offer
     * - Adds success or error flash messages
     */
    @GetMapping("/delete/{id}")
    public String deleteProductOffer(@PathVariable Long id,
                                     RedirectAttributes ra) {
        try {
            String url = API_BASE + "/offers/product/" + id;
            restTemplate.delete(url);
            ra.addFlashAttribute("successMsg", "Product offer deleted successfully.");
        } catch (Exception ex) {
            log.error("Failed to delete product offer {}", id, ex);
            ra.addFlashAttribute("errorMsg", "Failed to delete product offer.");
        }
        return "redirect:/admin/offers/product";
    }
}
