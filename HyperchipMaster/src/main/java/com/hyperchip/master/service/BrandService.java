package com.hyperchip.master.service;

import com.hyperchip.master.model.Brand;
import com.hyperchip.common.dto.BrandDto;
import com.hyperchip.common.dto.PageBrandDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

/**
 * Service interface for managing brands in the system.
 * Provides methods for:
 * - Creating/updating brands (with optional image upload)
 * - Soft deleting brands
 * - Listing brands with search, pagination, and sorting
 * - Retrieving a brand by ID
 */
public interface BrandService {

    /**
     * Create a new brand.
     * @param brand Brand entity containing basic info
     * @param file Optional image file for the brand
     * @return Saved Brand entity
     * @throws IOException if file upload fails
     */
    Brand saveBrand(Brand brand, MultipartFile file) throws IOException;

    /**
     * Update an existing brand by ID.
     * Can also update brand image if provided.
     * @param brand Brand entity with updated info
     * @param file Optional image file
     * @return Updated Brand entity
     * @throws IOException if file upload fails
     */
    Brand updateBrand(Brand brand, MultipartFile file) throws IOException;

    /**
     * Soft delete a brand.
     * Marks the brand as deleted without removing it from DB.
     * @param id Brand ID
     * @return Soft-deleted Brand entity
     */
    Brand softDeleteBrand(Long id);

    /**
     * List brands with optional search query, pagination, and sorting.
     * @param q Search query (can be null/empty)
     * @param page Page number (0-based)
     * @param size Page size
     * @return PageBrandDto containing paginated list of BrandDto
     */
    PageBrandDto listBrands(String q, int page, int size);

    /**
     * Retrieve a brand by ID.
     * @param id Brand ID
     * @return Optional containing Brand if found and not deleted
     */
    Optional<Brand> getBrandById(Long id);
}
