package com.hyperchip.admin.controller;

import com.hyperchip.common.dto.CategoryDto;
import com.hyperchip.common.dto.PageCategoryDto;
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
 * CategoryController
 *
 * Purpose:
 * Handles all Admin-side operations for Category management.
 * Communicates with the Master service to perform CRUD operations.
 * Provides pages for listing, adding, editing, deleting, and searching categories.
 */
@Controller
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class CategoryController {

    // ===================================================
    // LIST CATEGORIES PAGE
    // ===================================================
    /**
     * Purpose:
     * Display paginated list of categories in Admin UI.
     *
     * What it does:
     * - Fetches categories from Master service
     * - Supports optional search query `q`
     * - Sets model attributes for Thymeleaf rendering
     *
     * Why it is used:
     * Allows admin to view, search, and paginate categories efficiently.
     */
    @GetMapping
    public String listCategories(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Model model,
            HttpServletResponse response
    ) {
        // Disable caching for category page
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        // Fetch paginated categories from Master service
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://localhost:8086/api/admin/categories?page=" + page + "&size=" + size;
        if (q != null && !q.isEmpty()) url += "&q=" + q;

        PageCategoryDto categories = restTemplate.getForObject(url, PageCategoryDto.class);

        // Add data to model for Thymeleaf
        model.addAttribute("categories", categories);
        model.addAttribute("q", q);

        return "admin/category-list";
    }

    // ===================================================
    // SHOW ADD CATEGORY FORM
    // ===================================================
    /**
     * Purpose:
     * Display the form to add a new category.
     *
     * What it does:
     * - Initializes empty CategoryDto if not present
     * - Fetches all categories for reference in UI
     */
    @GetMapping("/add")
    public String showAddCategoryForm(Model model) {
        if (!model.containsAttribute("category")) {
            model.addAttribute("category", new CategoryDto());
        }

        RestTemplate restTemplate = new RestTemplate();
        PageCategoryDto page = restTemplate.getForObject(
                "http://localhost:8086/api/admin/categories?page=0&size=1000",
                PageCategoryDto.class
        );

        model.addAttribute("allCategories",
                page != null && page.getContent() != null ? page.getContent() : List.of());

        return "admin/category-form";
    }

    // ===================================================
    // SHOW EDIT CATEGORY FORM
    // ===================================================
    /**
     * Purpose:
     * Display the form to edit an existing category.
     *
     * What it does:
     * - Fetches the category by id from Master service
     * - Loads all categories for UI reference
     */
    @GetMapping("/edit/{id}")
    public String editCategory(@PathVariable Long id, Model model) {
        RestTemplate restTemplate = new RestTemplate();

        // Fetch the category details
        CategoryDto category = restTemplate.getForObject(
                "http://localhost:8086/api/admin/categories/" + id,
                CategoryDto.class
        );
        model.addAttribute("category", category);

        // Fetch all categories for UI reference
        PageCategoryDto page = restTemplate.getForObject(
                "http://localhost:8086/api/admin/categories?page=0&size=1000",
                PageCategoryDto.class
        );
        model.addAttribute("allCategories",
                page != null && page.getContent() != null ? page.getContent() : List.of());

        return "admin/category-form";
    }

    // ===================================================
    // SAVE CATEGORY (ADD)
    // ===================================================
    /**
     * Purpose:
     * Saves a new category with optional image upload.
     *
     * What it does:
     * - Collects category data and file in a MultiValueMap
     * - Calls Master service to create the category
     * - Handles duplicate category error (400 response)
     */
    @PostMapping("/save")
    public String saveCategory(@ModelAttribute("category") CategoryDto category,
                               @RequestParam("imageFile") MultipartFile file,
                               RedirectAttributes redirectAttributes) {

        RestTemplate restTemplate = new RestTemplate();
        MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        map.add("id", category.getId());
        map.add("name", category.getName());
        map.add("active", category.getActive());

        try {
            // Add image file if present
            if (file != null && !file.isEmpty()) {
                map.add("imageFile", new MultipartInputStreamFileResource(
                        file.getInputStream(), file.getOriginalFilename()));
            }

            // Call Master service to create category
            restTemplate.postForObject("http://localhost:8086/api/admin/categories", map, String.class);

            redirectAttributes.addFlashAttribute(
                    "successMsg",
                    category.getName() + " added successfully."
            );

            return "redirect:/admin/categories";

        } catch (HttpClientErrorException.BadRequest ex) {
            // Handle duplicate category error
            String apiMessage = ex.getResponseBodyAsString();
            redirectAttributes.addFlashAttribute(
                    "errorMsg",
                    (apiMessage != null && !apiMessage.isBlank()) ? apiMessage : "Category already exists with this name."
            );
            redirectAttributes.addFlashAttribute("category", category);
            return "redirect:/admin/categories/add";

        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMsg", "Something went wrong while saving the category.");
            redirectAttributes.addFlashAttribute("category", category);
            return "redirect:/admin/categories/add";
        }
    }

    // ===================================================
    // UPDATE CATEGORY
    // ===================================================
    /**
     * Purpose:
     * Updates an existing category with optional image upload.
     *
     * What it does:
     * - Sends updated data to Master service using PUT
     * - Handles duplicate category error
     */
    @PostMapping("/update")
    public String updateCategory(@ModelAttribute("category") CategoryDto category,
                                 @RequestParam("imageFile") MultipartFile file,
                                 RedirectAttributes redirectAttributes) {

        RestTemplate restTemplate = new RestTemplate();
        MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        map.add("id", category.getId());
        map.add("name", category.getName());
        map.add("active", category.getActive());

        try {
            if (file != null && !file.isEmpty()) {
                map.add("imageFile", new MultipartInputStreamFileResource(
                        file.getInputStream(), file.getOriginalFilename()));
            }

            restTemplate.put("http://localhost:8086/api/admin/categories/" + category.getId(), map);

            redirectAttributes.addFlashAttribute(
                    "successMsg",
                    category.getName() + " updated successfully."
            );

            return "redirect:/admin/categories";

        } catch (HttpClientErrorException.BadRequest ex) {
            String apiMessage = ex.getResponseBodyAsString();
            redirectAttributes.addFlashAttribute(
                    "errorMsg",
                    (apiMessage != null && !apiMessage.isBlank()) ? apiMessage : "Category already exists with this name."
            );
            redirectAttributes.addFlashAttribute("category", category);

            return "redirect:/admin/categories/edit/" + category.getId();

        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMsg", "Something went wrong while updating the category.");
            redirectAttributes.addFlashAttribute("category", category);

            return "redirect:/admin/categories/edit/" + category.getId();
        }
    }

    // ===================================================
    // DELETE CATEGORY
    // ===================================================
    /**
     * Purpose:
     * Deletes a category by ID.
     *
     * What it does:
     * - Fetches category details for success message
     * - Calls Master service to delete category
     * - Handles errors gracefully
     */
    @GetMapping("/delete/{id}")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        RestTemplate restTemplate = new RestTemplate();
        try {
            CategoryDto category = restTemplate.getForObject(
                    "http://localhost:8086/api/admin/categories/" + id,
                    CategoryDto.class
            );

            restTemplate.delete("http://localhost:8086/api/admin/categories/" + id);

            redirectAttributes.addFlashAttribute("successMsg",
                    category.getName() + " deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to delete category. Please try again.");
        }

        return "redirect:/admin/categories";
    }

    // ===================================================
    // RESET SEARCH
    // ===================================================
    /**
     * Purpose:
     * Resets search filters and redirects to category list.
     */
    @GetMapping("/reset")
    public String resetSearch() {
        return "redirect:/admin/categories";
    }
}
