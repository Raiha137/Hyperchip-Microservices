package com.hyperchip.master.service;

import com.hyperchip.common.dto.CategoryDto;
import com.hyperchip.master.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing product categories.
 * This service is mainly used by the Admin side for:
 * - Creating and updating categories (with optional image upload)
 * - Soft deleting categories
 * - Listing categories with search and pagination
 * - Fetching categories for dropdowns and JSON responses
 */
public interface CategoryService {

    /**
     * Create a new category.
     * Supports optional image upload for category thumbnail/banner.
     *
     * @param category Category entity containing name and metadata
     * @param file Optional image file
     * @return Saved Category entity
     * @throws IOException if image upload fails
     */
    Category saveCategory(Category category, MultipartFile file) throws IOException;

    /**
     * Update an existing category.
     * Can update both category details and image if provided.
     *
     * @param category Category entity with updated data
     * @param file Optional image file
     * @return Updated Category entity
     * @throws IOException if image upload fails
     */
    Category updateCategory(Category category, MultipartFile file) throws IOException;

    /**
     * Soft delete a category.
     * Marks the category as deleted instead of removing it from the database.
     * This ensures referential safety with products linked to the category.
     *
     * @param id Category ID
     * @return Soft-deleted Category entity
     */
    Category softDeleteCategory(Long id);

    /**
     * List categories with search and pagination support.
     * Used by Admin Category Management screen.
     *
     * @param q Search keyword (can be null or empty)
     * @param page Page number (0-based index)
     * @param size Number of records per page
     * @return Page of Category entities
     */
    Page<Category> listCategories(String q, int page, int size);

    /**
     * Retrieve a category by its ID.
     * Used in edit screens and internal validations.
     *
     * @param id Category ID
     * @return Optional containing Category if found and not deleted
     */
    Optional<Category> getCategoryById(Long id);

    /**
     * Fetch all active (non-deleted) categories as DTOs.
     * Used mainly for:
     * - Admin dropdowns
     * - Product creation forms
     * - JSON responses in AdminCategoryController
     *
     * @return List of CategoryDto
     */
    List<CategoryDto> getAllCategories();
}
