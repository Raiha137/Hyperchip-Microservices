package com.hyperchip.master.controller;

import com.hyperchip.common.dto.PageProductDto;
import com.hyperchip.common.dto.ProductDto;
import com.hyperchip.master.model.Brand;
import com.hyperchip.master.model.Category;
import com.hyperchip.master.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * ProductRestController
 *
 * Purpose:
 * - Provide public REST endpoints for products.
 * - Supports listing products with optional filters (keyword, category, brand, price range, pagination, sorting).
 * - Supports fetching details of a single product by its ID.
 */
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductRestController {

    private final ProductService productService; // Service layer handling product business logic

    // ===========================
    // LIST PRODUCTS WITH FILTERS
    // ===========================
    /**
     * GET /products
     *
     * Purpose:
     * - Retrieve a paginated list of products with optional filters.
     * - Filters include keyword search, category ID, brand ID, price range, page/size, and sorting.
     *
     * @param keyword optional search keyword (matches title/description)
     * @param categoryId optional category ID filter
     * @param brandId optional brand ID filter
     * @param minPrice optional minimum price filter
     * @param maxPrice optional maximum price filter
     * @param page pagination page number (default 0)
     * @param size pagination page size (default 12)
     * @param sortBy field to sort by (default "id")
     * @param sortDir sort direction: "asc" or "desc" (default "desc")
     * @return PageProductDto containing filtered, paginated products
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
        // Map categoryId/brandId to entity objects (service layer expects full entities)
        Category category = null;
        Brand brand = null;

        if (categoryId != null) {
            category = new Category();
            category.setId(categoryId);
        }
        if (brandId != null) {
            brand = new Brand();
            brand.setId(brandId);
        }

        // Call service layer to fetch paginated products based on filters
        return productService.listProductDtosPage(
                keyword, category, brand, minPrice, maxPrice, page, size, sortBy, sortDir
        );
    }

    // ===========================
    // GET SINGLE PRODUCT
    // ===========================
    /**
     * GET /products/{id}
     *
     * Purpose:
     * - Retrieve detailed information of a single product by its ID.
     * - Used for product detail pages or user-service requests.
     *
     * @param id the ID of the product to fetch
     * @return ProductDto containing product details
     */
    @GetMapping("/{id}")
    public ProductDto getOne(@PathVariable Long id) {
        return productService.getProductDtoById(id);
    }
}
