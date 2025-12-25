package com.hyperchip.master.controller;

import com.hyperchip.master.model.Category;
import com.hyperchip.master.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * ApiCategoryController
 *
 * Purpose:
 * - Exposes REST APIs for managing category entities in the admin panel.
 * - Supports full CRUD operations: Create, Read, Update, Delete.
 * - Handles optional image upload for category image/icon.
 */
@RestController
@RequestMapping("/api/admin/categories") // Base path for all category management APIs
@RequiredArgsConstructor
public class ApiCategoryController {

    private final CategoryService categoryService; // Service layer handling category business logic

    // ===========================
    // CREATE CATEGORY
    // ===========================
    /**
     * POST /api/admin/categories
     *
     * Purpose:
     * - Create a new category.
     * - Optionally upload a category image/icon.
     *
     * @param category Category entity details
     * @param file Optional image file for the category
     * @return Created Category object or error message
     */
    @PostMapping
    public ResponseEntity<?> createCategory(
            @ModelAttribute Category category,
            @RequestParam(name = "imageFile", required = false) MultipartFile file) {
        try {
            category.setDeleted(false); // Ensure the new category is marked as not deleted
            return ResponseEntity.ok(categoryService.saveCategory(category, file));
        } catch (Exception e) {
            // Return HTTP 400 if creation fails
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ===========================
    // UPDATE CATEGORY
    // ===========================
    /**
     * PUT /api/admin/categories/{id}
     *
     * Purpose:
     * - Update an existing category by ID.
     * - Optionally update the category image/icon.
     *
     * @param id Category ID to update
     * @param category Updated category details
     * @param file Optional new image file
     * @return Updated Category object or error message
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(
            @PathVariable(name = "id") Long id,
            @ModelAttribute Category category,
            @RequestParam(name = "imageFile", required = false) MultipartFile file) {
        try {
            category.setId(id); // Ensure the category entity has correct ID
            return ResponseEntity.ok(categoryService.updateCategory(category, file));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ===========================
    // DELETE CATEGORY (SOFT DELETE)
    // ===========================
    /**
     * DELETE /api/admin/categories/{id}
     *
     * Purpose:
     * - Soft delete a category by ID (mark as deleted instead of permanent removal)
     *
     * @param id Category ID to delete
     * @return Success message or error
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable(name = "id") Long id) {
        try {
            categoryService.softDeleteCategory(id); // Delegate to service for soft delete
            return ResponseEntity.ok("Category deleted successfully!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ===========================
    // GET CATEGORY BY ID
    // ===========================
    /**
     * GET /api/admin/categories/{id}
     *
     * Purpose:
     * - Retrieve a single category by its ID.
     *
     * @param id Category ID
     * @return Category entity or error if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getCategoryById(@PathVariable(name = "id") Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    // ===========================
    // LIST CATEGORIES WITH SEARCH & PAGINATION
    // ===========================
    /**
     * GET /api/admin/categories
     *
     * Purpose:
     * - List categories with optional search query.
     * - Supports pagination.
     *
     * @param q Optional search query for category name
     * @param page Page number (default 0)
     * @param size Page size (default 5)
     * @return Paginated list of categories
     */
    @GetMapping
    public ResponseEntity<?> listCategories(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size) {
        return ResponseEntity.ok(categoryService.listCategories(q, page, size));
    }
}
