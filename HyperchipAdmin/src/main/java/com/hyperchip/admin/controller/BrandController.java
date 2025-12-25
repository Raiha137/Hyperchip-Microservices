package com.hyperchip.admin.controller;

import com.hyperchip.common.dto.BrandDto;
import com.hyperchip.common.dto.PageBrandDto;
import com.hyperchip.common.util.MultipartInputStreamFileResource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * BrandController
 *
 * Purpose:
 * Handles all Admin-side operations for Brand management.
 * This controller communicates with the backend Brand API service
 * and provides pages for listing, adding, editing, deleting, and searching brands.
 */
@Controller
@RequestMapping("/admin/brands")
@RequiredArgsConstructor
public class BrandController {

    // ===================================================
    // LIST BRANDS PAGE
    // ===================================================
    /**
     * Purpose:
     * Display paginated brand list in the Admin UI.
     *
     * What it does:
     * - Calls Brand API service to fetch paginated brands
     * - Supports optional search query `q`
     * - Sets model attributes for Thymeleaf rendering
     *
     * Why it is used:
     * Allows admin to view brands and search/filter them efficiently.
     */
    @GetMapping
    public String listBrands(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size,
            Model model,
            HttpServletResponse response
    ) {
        // Prevent caching of brand list page
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        RestTemplate restTemplate = new RestTemplate();
        String url = "http://localhost:8086/api/admin/brands?page=" + page + "&size=" + size;
        if (q != null && !q.isEmpty()) url += "&q=" + q;

        PageBrandDto brands;
        try {
            brands = restTemplate.getForObject(url, PageBrandDto.class);
        } catch (Exception e) {
            brands = null; // handle failure gracefully
        }

        // Initialize empty page if API call fails
        if (brands == null) {
            brands = new PageBrandDto();
            brands.setContent(List.of());
            brands.setTotalElements(0L);
            brands.setTotalPages(0);
            brands.setNumber(page);
            brands.setSize(size);
            brands.setFirst(true);
            brands.setLast(true);
        }

        model.addAttribute("brands", brands);
        model.addAttribute("q", q);

        return "admin/brand-list";
    }

    // ===================================================
    // SHOW ADD BRAND FORM
    // ===================================================
    /**
     * Purpose:
     * Display the "Add Brand" form.
     *
     * What it does:
     * - Initializes empty BrandDto if not present
     * - Fetches all brands to show in UI (optional reference)
     */
    @GetMapping("/add")
    public String showAddBrandForm(Model model) {
        if (!model.containsAttribute("brand")) {
            model.addAttribute("brand", new BrandDto());
        }

        RestTemplate restTemplate = new RestTemplate();
        PageBrandDto page = restTemplate.getForObject(
                "http://localhost:8086/api/admin/brands?page=0&size=1000",
                PageBrandDto.class
        );

        model.addAttribute("allBrands",
                page != null && page.getContent() != null ? page.getContent() : List.of());

        return "admin/brand-form";
    }

    // ===================================================
    // SHOW EDIT BRAND FORM
    // ===================================================
    /**
     * Purpose:
     * Display the "Edit Brand" form for a given brand id.
     *
     * What it does:
     * - Fetches the brand by id from Brand API
     * - Loads all brands for UI reference
     */
    @GetMapping("/edit/{id}")
    public String editBrand(@PathVariable Long id, Model model) {
        RestTemplate restTemplate = new RestTemplate();

        BrandDto brand = restTemplate.getForObject(
                "http://localhost:8086/api/admin/brands/" + id,
                BrandDto.class
        );
        model.addAttribute("brand", brand);

        PageBrandDto page = restTemplate.getForObject(
                "http://localhost:8086/api/admin/brands?page=0&size=1000",
                PageBrandDto.class
        );
        model.addAttribute("allBrands",
                page != null && page.getContent() != null ? page.getContent() : List.of());

        return "admin/brand-form";
    }

    // ===================================================
    // SAVE BRAND (ADD OR UPDATE)
    // ===================================================
    /**
     * Purpose:
     * Saves a brand (new or existing) with optional image upload.
     *
     * What it does:
     * - Collects brand data and file in a MultiValueMap
     * - Calls Brand API service to create or update brand
     * - Handles duplicate brand error
     */
    @PostMapping("/save")
    public String saveBrand(@ModelAttribute("brand") BrandDto brand,
                            @RequestParam(value = "imageFile", required = false) MultipartFile file,
                            RedirectAttributes redirectAttributes) {

        RestTemplate restTemplate = new RestTemplate();
        MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();

        if (brand.getActive() == null) brand.setActive(true); // default active

        map.add("id", brand.getId());
        map.add("name", brand.getName());
        map.add("active", brand.getActive());

        try {
            // Add file if present
            if (file != null && !file.isEmpty()) {
                map.add("imageFile",
                        new MultipartInputStreamFileResource(file.getInputStream(),
                                file.getOriginalFilename()));
            }

            // CREATE or UPDATE
            if (brand.getId() == null) {
                restTemplate.postForObject("http://localhost:8086/api/admin/brands", map, String.class);
                redirectAttributes.addFlashAttribute("successMsg", brand.getName() + " added successfully.");
            } else {
                restTemplate.put("http://localhost:8086/api/admin/brands/" + brand.getId(), map);
                redirectAttributes.addFlashAttribute("successMsg", brand.getName() + " updated successfully.");
            }

            return "redirect:/admin/brands?page=0";

        } catch (HttpClientErrorException.BadRequest ex) {
            // Handle duplicate brand name or validation errors
            String apiMessage = ex.getResponseBodyAsString();
            redirectAttributes.addFlashAttribute(
                    "errorMsg",
                    (apiMessage != null && !apiMessage.isBlank()) ? apiMessage : "Brand already exists!"
            );
            redirectAttributes.addFlashAttribute("brand", brand);

            return (brand.getId() == null) ? "redirect:/admin/brands/add"
                    : "redirect:/admin/brands/edit/" + brand.getId();

        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMsg", "Something went wrong while saving the brand.");
            redirectAttributes.addFlashAttribute("brand", brand);
            return (brand.getId() == null) ? "redirect:/admin/brands/add"
                    : "redirect:/admin/brands/edit/" + brand.getId();
        }
    }

    // ===================================================
    // DELETE BRAND
    // ===================================================
    /**
     * Purpose:
     * Deletes a brand by id.
     *
     * What it does:
     * - Fetches brand info for success message
     * - Calls Brand API to delete
     * - Handles API errors
     */
    @GetMapping("/delete/{id}")
    public String deleteBrand(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        RestTemplate restTemplate = new RestTemplate();
        try {
            BrandDto brand = restTemplate.getForObject(
                    "http://localhost:8086/api/admin/brands/" + id,
                    BrandDto.class
            );
            restTemplate.delete("http://localhost:8086/api/admin/brands/" + id);
            redirectAttributes.addFlashAttribute("successMsg", brand.getName() + " deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to delete brand. Please try again.");
        }
        return "redirect:/admin/brands";
    }

    // ===================================================
    // RESET SEARCH
    // ===================================================
    /**
     * Purpose:
     * Resets any search/filter applied and redirects to brand list.
     */
    @GetMapping("/reset")
    public String resetSearch() {
        return "redirect:/admin/brands";
    }
}
