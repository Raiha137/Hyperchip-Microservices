// src/main/java/com/hyperchip/master/service/ProductService.java
package com.hyperchip.master.service;

import com.hyperchip.common.dto.ProductDto;
import com.hyperchip.common.dto.PageProductDto;
import com.hyperchip.master.model.Brand;
import com.hyperchip.master.model.Category;
import com.hyperchip.master.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * ProductService
 *
 * This service defines all business operations related to products, including:
 * - Product creation and updates
 * - Image upload and removal
 * - Soft deletion
 * - Product listing with search, filter, sort, and pagination
 * - Stock management
 *
 * It acts as the core product business layer used by both
 * Admin-side and User-side controllers.
 */
public interface ProductService {

    // ========================= PRODUCT CREATION =========================

    /**
     * Create and save a new product along with multiple uploaded images.
     *
     * Used by:
     * - Admin Product Management (Add Product)
     *
     * Responsibilities:
     * - Validate product data
     * - Upload and store image files
     * - Persist product with image references
     *
     * @param productDto      Product details from UI
     * @param uploadedImages  Array of uploaded image files
     * @return Saved ProductDto
     * @throws IOException if image storage fails
     */
    ProductDto saveProduct(ProductDto productDto,
                           MultipartFile[] uploadedImages) throws IOException;


    // ========================= PRODUCT UPDATE =========================

    /**
     * Update an existing product along with image modifications.
     *
     * Used by:
     * - Admin Product Management (Edit Product)
     *
     * Responsibilities:
     * - Update product fields
     * - Add newly uploaded images
     * - Remove selected existing images
     *
     * @param product             Updated product entity
     * @param uploadedImages      New images to be added
     * @param removedImageNames   Names of images to be deleted
     * @return Updated Product
     * @throws IOException if image handling fails
     */
    Product updateProduct(Product product,
                          List<MultipartFile> uploadedImages,
                          List<String> removedImageNames) throws IOException;

    /**
     * Update product details without modifying images.
     *
     * Used by:
     * - Admin updates where only textual or pricing data changes
     *
     * @param product Updated product entity
     * @return Updated Product
     */
    Product updateProductWithoutImages(Product product);


    // ========================= PRODUCT DELETION =========================

    /**
     * Soft delete a product.
     *
     * Business rule:
     * - Product is not removed from DB
     * - Marked as deleted and hidden from user listings
     *
     * Used by:
     * - Admin Product Management (Delete Product)
     *
     * @param id Product ID
     * @return Soft-deleted Product
     */
    Product softDeleteProduct(Long id);


    // ========================= PRODUCT LISTING =========================

    /**
     * List products with advanced search, filter, sort, and pagination.
     *
     * Used by:
     * - Admin product list
     * - User product listing pages
     *
     * Supports:
     * - Keyword search
     * - Category & brand filtering
     * - Price range filtering
     * - Sorting and pagination
     *
     * @param keyword   Search keyword
     * @param category  Filter by category
     * @param brand     Filter by brand
     * @param minPrice  Minimum price filter
     * @param maxPrice  Maximum price filter
     * @param page      Page number
     * @param size      Page size
     * @param sortBy    Field to sort by
     * @param sortDir   Sort direction (ASC/DESC)
     * @return Page of Product entities
     */
    Page<Product> listProducts(String keyword,
                               Category category,
                               Brand brand,
                               Double minPrice,
                               Double maxPrice,
                               int page,
                               int size,
                               String sortBy,
                               String sortDir);


    // ========================= PRODUCT FETCH =========================

    /**
     * Fetch a product by ID (only if not deleted).
     *
     * Used by:
     * - Admin product edit view
     * - Product detail page
     *
     * @param id Product ID
     * @return Optional Product
     */
    Optional<Product> getProductById(Long id);

    /**
     * Check if a product title already exists (case-insensitive).
     *
     * Used by:
     * - Admin validation during product creation
     *
     * @param title Product title
     * @return true if exists, false otherwise
     */
    boolean existsByTitle(String title);


    // ========================= PRODUCT DTO LISTING =========================

    /**
     * List products as DTOs without pagination metadata.
     *
     * Used by:
     * - Lightweight API responses
     * - Internal service calls
     *
     * @return List of ProductDto
     */
    List<ProductDto> listProductDtos(String keyword,
                                     Category category,
                                     Brand brand,
                                     Double minPrice,
                                     Double maxPrice,
                                     int page,
                                     int size,
                                     String sortBy,
                                     String sortDir);

    /**
     * List products as DTOs with pagination metadata.
     *
     * Used by:
     * - User-facing APIs
     * - Admin product listing APIs
     *
     * @return PageProductDto containing product list and page info
     */
    PageProductDto listProductDtosPage(String keyword,
                                       Category category,
                                       Brand brand,
                                       Double minPrice,
                                       Double maxPrice,
                                       int page,
                                       int size,
                                       String sortBy,
                                       String sortDir);

    /**
     * Fetch a single product as ProductDto.
     *
     * Used by:
     * - Product detail page
     * - Admin product edit page
     *
     * @param id Product ID
     * @return ProductDto
     */
    ProductDto getProductDtoById(Long id);


    // ========================= STOCK MANAGEMENT =========================

    /**
     * Increase product stock quantity.
     *
     * Used by:
     * - Admin stock updates
     * - Order cancellation / returns
     *
     * @param productId Product ID
     * @param quantity  Quantity to add
     */
    void incrementStock(Long productId, int quantity);

    /**
     * Decrease product stock quantity.
     *
     * Used by:
     * - Order placement
     * - Inventory reservation
     *
     * @param productId Product ID
     * @param quantity  Quantity to subtract
     */
    void decrementStock(Long productId, int quantity);


    // ========================= IMAGE MANAGEMENT =========================

    /**
     * Remove a specific image from a product.
     *
     * Responsibilities:
     * - Delete image file from storage
     * - Remove image reference from product entity
     *
     * Used by:
     * - Admin product image management
     *
     * @param product  Product entity
     * @param filename Image filename to remove
     * @throws IOException if file deletion fails
     */
    void removeImageFromProduct(Product product,
                                String filename) throws IOException;
}
