package com.hyperchip.master.controller;

import com.hyperchip.common.dto.PageProductDto;
import com.hyperchip.common.dto.ProductDto;
import com.hyperchip.master.model.Brand;
import com.hyperchip.master.model.Category;
import com.hyperchip.master.model.Product;
import com.hyperchip.master.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * AdminProductApiController
 *
 * Purpose:
 * - Exposes REST APIs for managing products in the admin panel.
 * - Supports listing, retrieving, creating, updating, and deleting products.
 * - Handles image uploads/removals via multipart/form-data.
 * - Supports filtering, pagination, and sorting for product listings.
 */
@RestController
@RequestMapping("/api/admin/products") // Base path for all product management APIs
@RequiredArgsConstructor
public class AdminProductApiController {

    private final ProductService productService; // Service layer handling product business logic

    // =====================================================
    // LIST PRODUCTS WITH FILTERS, PAGINATION, AND SORTING
    // =====================================================
    /**
     * GET /api/admin/products
     *
     * Purpose:
     * - List products with optional filters: keyword, category, brand, price range
     * - Supports pagination and sorting
     *
     * @param keyword   Optional search keyword (matches title/description)
     * @param categoryId Optional category filter
     * @param brandId    Optional brand filter
     * @param minPrice   Optional minimum price
     * @param maxPrice   Optional maximum price
     * @param page       Page number (default 0)
     * @param size       Page size (default 12)
     * @param sortBy     Field to sort by (default "id")
     * @param sortDir    Sort direction (asc/desc, default "desc")
     * @return PageProductDto containing paginated products
     */
    @GetMapping
    public PageProductDto listProducts(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "category", required = false) Long categoryId,
            @RequestParam(name = "brand", required = false) Long brandId,
            @RequestParam(name = "minPrice", required = false) Double minPrice,
            @RequestParam(name = "maxPrice", required = false) Double maxPrice,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size,
            @RequestParam(name = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir
    ) {
        // Convert categoryId and brandId to entity objects for filtering
        Category category = categoryId != null ? new Category() {{ setId(categoryId); }} : null;
        Brand brand = brandId != null ? new Brand() {{ setId(brandId); }} : null;

        // Call service method to get paginated and filtered product list
        return productService.listProductDtosPage(
                keyword, category, brand, minPrice, maxPrice, page, size, sortBy, sortDir
        );
    }

    // ===========================
    // GET SINGLE PRODUCT BY ID
    // ===========================
    /**
     * GET /api/admin/products/{id}
     *
     * Purpose:
     * - Retrieve a single product by its ID
     * - Returns ProductDto containing all details
     *
     * @param id Product ID
     * @return ProductDto of the requested product
     */
    @GetMapping("/{id}")
    public ProductDto getOne(@PathVariable Long id) {
        return productService.getProductDtoById(id);
    }

    // ===========================
    // CREATE PRODUCT (WITH IMAGES)
    // ===========================
    /**
     * POST /api/admin/products
     *
     * Purpose:
     * - Create a new product
     * - Supports multipart/form-data for uploading product images
     *
     * @param productDto Product details
     * @param categoryId Optional category ID
     * @param brandId    Optional brand ID
     * @param uploadedImages Optional array of images to upload
     * @param removedImageNames Not used here, placeholder for future
     * @return Created ProductDto (HTTP 201)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createProduct(
            @RequestPart("product") ProductDto productDto,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "brandId", required = false) Long brandId,
            @RequestParam(name = "uploadedImages", required = false) MultipartFile[] uploadedImages,
            @RequestParam(name = "removedImageNames", required = false) List<String> removedImageNames
    ) throws IOException {

        // Set category and brand IDs in DTO if provided
        if (categoryId != null) productDto.setCategoryId(categoryId);
        if (brandId != null) productDto.setBrandId(brandId);

        // Delegate to service layer for saving product and uploading images
        ProductDto saved = productService.saveProduct(productDto, uploadedImages);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ===========================
    // UPDATE PRODUCT (WITH IMAGES)
    // ===========================
    /**
     * PUT /api/admin/products/{id}
     *
     * Purpose:
     * - Update an existing product
     * - Supports multipart/form-data for adding/removing images
     *
     * @param id Product ID to update
     * @param productDto Product details
     * @param categoryId Optional new category ID
     * @param brandId Optional new brand ID
     * @param uploadedImages Optional new images to add
     * @param removedImageNames Optional images to remove
     * @return Updated Product entity
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProduct(
            @PathVariable Long id,
            @RequestPart("product") ProductDto productDto,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "brandId", required = false) Long brandId,
            @RequestParam(name = "uploadedImages", required = false) MultipartFile[] uploadedImages,
            @RequestParam(name = "removedImageNames", required = false) List<String> removedImageNames
    ) throws IOException {

        // Map DTO to Product entity
        Product p = new Product();
        p.setId(id);
        p.setTitle(productDto.getTitle());
        p.setDescription(productDto.getDescription());
        p.setPrice(productDto.getPrice());
        p.setStock(productDto.getStock());
        p.setActive(productDto.getActive() != null ? productDto.getActive() : true);
        p.setDeleted(productDto.getDeleted() != null ? productDto.getDeleted() : false);

        // Map category
        if (categoryId != null || productDto.getCategoryId() != null) {
            Category c = new Category();
            c.setId(categoryId != null ? categoryId : productDto.getCategoryId());
            p.setCategory(c);
        }

        // Map brand
        if (brandId != null || productDto.getBrandId() != null) {
            Brand b = new Brand();
            b.setId(brandId != null ? brandId : productDto.getBrandId());
            p.setBrand(b);
        }

        // Convert uploaded images array to list
        List<MultipartFile> uploads = uploadedImages != null ? Arrays.asList(uploadedImages) : null;

        // Delegate to service layer to update product and handle image changes
        Product updated = productService.updateProduct(p, uploads, removedImageNames);
        return ResponseEntity.ok(updated);
    }

    // ===========================
    // DELETE A SPECIFIC IMAGE FROM A PRODUCT
    // ===========================
    /**
     * DELETE /api/admin/products/{id}/images/{filename}
     *
     * Purpose:
     * - Delete a single image from an existing product
     * - Returns HTTP 200 on success, 500 on failure
     *
     * @param id Product ID
     * @param filename Name of the image file to delete
     * @return Status message
     */
    @DeleteMapping("/{id}/images/{filename:.+}")
    public ResponseEntity<?> deleteProductImage(@PathVariable Long id, @PathVariable String filename) {
        try {
            Product product = productService.getProductById(id)
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            productService.removeImageFromProduct(product, filename);
            return ResponseEntity.ok("Image removed");
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to remove image: " + ex.getMessage());
        }
    }
}
