package com.hyperchip.admin.controller;

import com.hyperchip.common.dto.CategoryDto;
import com.hyperchip.common.dto.CategoryOfferDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.Collections;

/**
 * CategoryOfferAdminController
 *
 * Purpose:
 * Handles Admin-side operations for managing Category Offers.
 * Communicates with Master service for category data and Offer service for CRUD operations.
 */
@Controller
@RequestMapping("/admin/offers/category")
@Slf4j
public class CategoryOfferAdminController {

    // Base API URL for Offer service
    @Value("${api.base.url}")
    private String API_BASE;

    // Base URL for Master service (categories)
    @Value("${master.service.url}")
    private String MASTER_BASE;

    private final RestTemplate restTemplate = new RestTemplate();

    // ===================================================
    // LIST CATEGORY OFFERS
    // ===================================================
    /**
     * Purpose:
     * Display list of all category offers for admin.
     *
     * What it does:
     * - Calls the Offer service to get all category offers
     * - Adds the result to Thymeleaf model
     * - Handles errors gracefully
     */
    @GetMapping
    public String listCategoryOffers(Model model) {
        try {
            String url = API_BASE + "/offers/category";
            CategoryOfferDto[] offers = restTemplate.getForObject(url, CategoryOfferDto[].class);

            model.addAttribute("offers",
                    offers != null ? Arrays.asList(offers) : Collections.emptyList());
        } catch (Exception ex) {
            log.error("Failed to load category offers", ex);
            model.addAttribute("offers", Collections.emptyList());
            model.addAttribute("errorMsg", "Failed to load category offers.");
        }
        return "admin/category-offer-list";
    }

    // ===================================================
    // SHOW CREATE CATEGORY OFFER FORM
    // ===================================================
    /**
     * Purpose:
     * Display form to create a new category offer.
     *
     * What it does:
     * - Initializes empty CategoryOfferDto
     * - Fetches all categories for dropdown selection
     * - Handles errors gracefully
     */
    @GetMapping("/add")
    public String showCreateForm(Model model, RedirectAttributes ra) {
        model.addAttribute("offer", new CategoryOfferDto());
        try {
            String url = MASTER_BASE + "/api/admin/categories/all";
            CategoryDto[] categories = restTemplate.getForObject(url, CategoryDto[].class);

            model.addAttribute("categories",
                    categories != null ? Arrays.asList(categories) : Collections.emptyList());
        } catch (Exception ex) {
            log.error("Failed to load categories", ex);
            ra.addFlashAttribute("errorMsg", "Failed to load categories.");
        }
        return "admin/category-offer-form";
    }

    // ===================================================
    // SHOW EDIT CATEGORY OFFER FORM
    // ===================================================
    /**
     * Purpose:
     * Display form to edit an existing category offer.
     *
     * What it does:
     * - Fetches the category offer by ID
     * - Fetches all categories for dropdown
     * - Handles not found or other errors gracefully
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id,
                               Model model,
                               RedirectAttributes ra) {
        try {
            // Load existing offer
            String offerUrl = API_BASE + "/offers/category/" + id;
            CategoryOfferDto dto = restTemplate.getForObject(offerUrl, CategoryOfferDto.class);
            if (dto == null) {
                ra.addFlashAttribute("errorMsg", "Category offer not found.");
                return "redirect:/admin/offers/category";
            }
            model.addAttribute("offer", dto);

            // Load categories for dropdown
            String catUrl = MASTER_BASE + "/api/admin/categories/all";
            CategoryDto[] categories = restTemplate.getForObject(catUrl, CategoryDto[].class);
            model.addAttribute("categories",
                    categories != null ? Arrays.asList(categories) : Collections.emptyList());

            return "admin/category-offer-form";
        } catch (Exception ex) {
            log.error("Failed to load category offer {}", id, ex);
            ra.addFlashAttribute("errorMsg", "Failed to load category offer.");
            return "redirect:/admin/offers/category";
        }
    }

    // ===================================================
    // SAVE CATEGORY OFFER (CREATE / UPDATE)
    // ===================================================
    /**
     * Purpose:
     * Save a new or updated category offer.
     *
     * What it does:
     * - Checks if the offer has an ID (create or update)
     * - Calls Offer service to persist the data
     * - Adds success or error messages to RedirectAttributes
     */
    @PostMapping("/save")
    public String saveCategoryOffer(@ModelAttribute("offer") CategoryOfferDto offer,
                                    RedirectAttributes ra) {
        try {
            String base = API_BASE + "/offers/category";

            if (offer.getId() == null) {
                // CREATE new offer
                restTemplate.postForObject(base, offer, CategoryOfferDto.class);
                ra.addFlashAttribute("successMsg", "Category offer created successfully.");
            } else {
                // UPDATE existing offer
                String url = base + "/" + offer.getId();
                restTemplate.put(url, offer);
                ra.addFlashAttribute("successMsg", "Category offer updated successfully.");
            }
        } catch (Exception ex) {
            log.error("Failed to save category offer", ex);
            ra.addFlashAttribute("errorMsg", "Failed to save category offer.");
        }
        return "redirect:/admin/offers/category";
    }

    // ===================================================
    // DELETE CATEGORY OFFER
    // ===================================================
    /**
     * Purpose:
     * Delete a category offer by ID.
     *
     * What it does:
     * - Calls Offer service to delete the offer
     * - Adds success or error message for admin UI
     */
    @GetMapping("/delete/{id}")
    public String deleteCategoryOffer(@PathVariable Long id,
                                      RedirectAttributes ra) {
        try {
            String url = API_BASE + "/offers/category/" + id;
            restTemplate.delete(url);
            ra.addFlashAttribute("successMsg", "Category offer deleted successfully.");
        } catch (Exception ex) {
            log.error("Failed to delete category offer {}", id, ex);
            ra.addFlashAttribute("errorMsg", "Failed to delete category offer.");
        }
        return "redirect:/admin/offers/category";
    }
}
