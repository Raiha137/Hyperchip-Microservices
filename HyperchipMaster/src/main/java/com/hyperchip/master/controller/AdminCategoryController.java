package com.hyperchip.master.controller;

import com.hyperchip.common.dto.CategoryDto;
import com.hyperchip.master.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AdminCategoryController
 *
 * Purpose:
 * - Expose REST APIs for admin to manage categories
 * - Currently provides a method to fetch all categories
 */
@RestController
@RequestMapping("/api/admin/categories")  // Base URL for admin category APIs
@RequiredArgsConstructor                     // Auto-generate constructor for final fields
public class AdminCategoryController {

    private final CategoryService categoryService;  // Service to handle category business logic

    /**
     * GET /api/admin/categories/all
     *
     * Purpose:
     * - Fetch all categories without pagination or filtering
     * - Used by admin UI to populate dropdowns or lists
     *
     * @return ResponseEntity with List of CategoryDto and HTTP 200 OK
     */
    @GetMapping("/all")
    public ResponseEntity<List<CategoryDto>> listAll() {
        List<CategoryDto> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }
}
