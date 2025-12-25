package com.hyperchip.master.controller;

import com.hyperchip.master.model.Brand;
import com.hyperchip.master.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * ApiBrandController
 *
 * Purpose:
 * - Exposes REST APIs for managing brand entities in the admin panel.
 * - Supports full CRUD operations: Create, Read, Update, Delete.
 * - Handles optional image upload for brand logo/image.
 */
@RestController
@RequestMapping("/api/admin/brands") // Base path for all brand management APIs
@RequiredArgsConstructor
public class ApiBrandController {

    private final BrandService brandService; // Service layer handling brand business logic

    // ===========================
    // CREATE BRAND
    // ===========================
    /**
     * POST /api/admin/brands
     *
     * Purpose:
     * - Create a new brand
     * - Optionally upload a brand image/logo
     *
     * @param brand Brand entity details
     * @param file Optional image file for the brand
     * @return Created Brand object or error message
     */
    @PostMapping
    public ResponseEntity<?> createBrand(
            @ModelAttribute Brand brand,
            @RequestParam(name = "imageFile", required = false) MultipartFile file) {
        try {
            // Ensure the new brand is marked as not deleted
            brand.setDeleted(false);
            // Delegate to service to save brand and upload image
            return ResponseEntity.ok(brandService.saveBrand(brand, file));
        } catch (Exception e) {
            // Return HTTP 400 with error message if creation fails
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ===========================
    // UPDATE BRAND
    // ===========================
    /**
     * PUT /api/admin/brands/{id}
     *
     * Purpose:
     * - Update an existing brand by ID
     * - Optionally update the brand image/logo
     *
     * @param id Brand ID to update
     * @param brand Updated brand details
     * @param file Optional new image file
     * @return Updated Brand object or error message
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateBrand(
            @PathVariable(name = "id") Long id,
            @ModelAttribute Brand brand,
            @RequestParam(name = "imageFile", required = false) MultipartFile file) {
        try {
            brand.setId(id); // Ensure the brand entity has correct ID
            return ResponseEntity.ok(brandService.updateBrand(brand, file));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ===========================
    // DELETE BRAND (SOFT DELETE)
    // ===========================
    /**
     * DELETE /api/admin/brands/{id}
     *
     * Purpose:
     * - Soft delete a brand by ID (mark as deleted instead of permanent removal)
     *
     * @param id Brand ID to delete
     * @return Success message or error
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBrand(@PathVariable(name = "id") Long id) {
        try {
            brandService.softDeleteBrand(id); // Delegate to service for soft delete
            return ResponseEntity.ok("Brand deleted successfully!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ===========================
    // GET BRAND BY ID
    // ===========================
    /**
     * GET /api/admin/brands/{id}
     *
     * Purpose:
     * - Retrieve a single brand by its ID
     *
     * @param id Brand ID
     * @return Brand entity or error if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getBrandById(@PathVariable(name = "id") Long id) {
        return ResponseEntity.ok(brandService.getBrandById(id));
    }

    // ===========================
    // LIST BRANDS WITH SEARCH & PAGINATION
    // ===========================
    /**
     * GET /api/admin/brands
     *
     * Purpose:
     * - List brands with optional search query
     * - Supports pagination
     *
     * @param q Optional search query for brand name
     * @param page Page number (default 0)
     * @param size Page size (default 5)
     * @return Paginated list of brands
     */
    @GetMapping
    public ResponseEntity<?> listBrands(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size) {
        return ResponseEntity.ok(brandService.listBrands(q, page, size));
    }
}
