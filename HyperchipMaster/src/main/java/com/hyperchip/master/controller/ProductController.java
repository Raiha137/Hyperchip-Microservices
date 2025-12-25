package com.hyperchip.master.controller;

import com.hyperchip.common.dto.PageProductDto;
import com.hyperchip.common.dto.ProductDto;
import com.hyperchip.master.model.Brand;
import com.hyperchip.master.model.Category;
import com.hyperchip.master.repository.BrandRepository;
import com.hyperchip.master.repository.CategoryRepository;
import com.hyperchip.master.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * ProductController
 *
 * Purpose:
 * - Expose public endpoints for products to be used by user-service or frontend.
 * - Provides listing with filters, fetching single products, and serving uploaded product images.
 */
@RestController
@RequestMapping("/public/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;              // Business logic for products
    private final CategoryRepository categoryRepository;      // For fetching categories by name
    private final BrandRepository brandRepository;            // For fetching brands by name

    @Value("${upload.dir:${upload.products.dir:uploads/products}}")
    private String uploadDir;                                 // Base upload directory for product images

    // ===========================
    // LIST PRODUCTS WITH FILTERS
    // ===========================
    /**
     * GET /public/products
     *
     * Purpose:
     * - List products for the public with optional filters:
     *   keyword, categoryName, brandName, price range, pagination, and sorting.
     * - Supports both text search and numeric price search.
     *
     * @param keyword optional search keyword
     * @param q optional alternative search keyword
     * @param categoryName optional category name filter
     * @param brandName optional brand name filter
     * @param minPrice optional minimum price
     * @param maxPrice optional maximum price
     * @param page pagination page number
     * @param size pagination page size
     * @param sortBy sorting field
     * @param sortDir sorting direction (asc/desc)
     * @return PageProductDto with filtered products
     */
    // ---- Replace the existing listPublicProducts(...) method with this ----
    @GetMapping
    public ResponseEntity<PageProductDto> listPublicProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, name = "q") String q,
            @RequestParam(required = false, name = "categoryName") String categoryName,
            @RequestParam(required = false, name = "brandName") String brandName,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {

        // small helper to URL-decode, normalize spaces and trim
        final java.util.function.Function<String, String> normalizeParam = s -> {
            if (s == null) return null;
            try {
                s = URLDecoder.decode(s, StandardCharsets.UTF_8.name());
            } catch (Exception ignored) { }
            s = s.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
            return s.isBlank() ? null : s;
        };

        // Determine actual search keyword
        String searchKeyword = null;
        if (keyword != null && !keyword.isBlank()) searchKeyword = keyword.trim();
        else if (q != null && !q.isBlank()) searchKeyword = q.trim();

        // normalize searchKeyword right away
        searchKeyword = normalizeParam.apply(searchKeyword);

        // If keyword looks like a numeric price, treat as min/max price
        Double numeric = null;
        if (searchKeyword != null) {
            String cleaned = searchKeyword.replaceAll("[^0-9\\.\\-]", "");
            if (!cleaned.isBlank()) {
                try {
                    numeric = Double.parseDouble(cleaned);
                } catch (NumberFormatException ignored) { }
            }
        }
        if (numeric != null) {
            minPrice = numeric;
            maxPrice = numeric;
            searchKeyword = null;
        }

        // Resolve Category entity if categoryName provided (normalize and decode first)
        Category category = null;
        String normalizedCategory = normalizeParam.apply(categoryName);
        if (normalizedCategory != null) {
            category = categoryRepository.findByNameIgnoreCaseAndDeletedFalse(normalizedCategory);
        }

        // Resolve Brand entity if brandName provided (normalize and decode first)
        Brand brand = null;
        String normalizedBrand = normalizeParam.apply(brandName);
        if (normalizedBrand != null) {
            brand = brandRepository.findByNameIgnoreCaseAndDeletedFalse(normalizedBrand);
        }

        // Fetch paginated products using service
        PageProductDto dto = productService.listProductDtosPage(
                searchKeyword,
                category,
                brand,
                minPrice,
                maxPrice,
                page,
                size,
                sortBy,
                sortDir
        );

        return ResponseEntity.ok(dto);
    }

    // ===========================
    // GET SINGLE PRODUCT
    // ===========================
    /**
     * GET /public/products/{id}
     *
     * Purpose:
     * - Fetch a single product by its ID.
     * - Used for product detail pages in the frontend or user-service.
     *
     * @param id product ID
     * @return ProductDto
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProduct(@PathVariable Long id) {
        ProductDto dto = productService.getProductDtoById(id);
        return ResponseEntity.ok(dto);
    }

    // ===========================
    // SERVE PRODUCT IMAGES
    // ===========================
    /**
     * GET /public/products/images/{filename}
     *
     * Purpose:
     * - Serve uploaded product images as public resources.
     * - Dynamically determines content type and returns appropriate MediaType.
     *
     * @param filename image filename
     * @return image as Resource
     */
    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<Resource> serveImage(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(filename);

            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(filePath.toUri());

            // Detect content type
            String contentType = Files.probeContentType(filePath);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(
                    contentType != null
                            ? MediaType.parseMediaType(contentType)
                            : MediaType.IMAGE_JPEG
            );

            return new ResponseEntity<>(resource, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
