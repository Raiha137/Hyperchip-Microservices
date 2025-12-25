// src/main/java/com/hyperchip/master/controller/PublicProductController.java
package com.hyperchip.master.controller;

import com.hyperchip.common.dto.ProductDto;
import com.hyperchip.master.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * PublicProductController
 *
 * Purpose:
 * - Expose public API endpoints to fetch product details.
 * - Specifically designed for frontend or other services to get product info.
 */
@RestController
@RequestMapping("/api/products") // Base path for public product APIs
@RequiredArgsConstructor           // Auto-generate constructor for final fields
public class PublicProductController {

    private final ProductService productService; // Service layer handling product operations

    // ===========================
    // GET SINGLE PRODUCT
    // ===========================
    /**
     * GET /api/products/{id}
     *
     * Purpose:
     * - Retrieve detailed information of a single product by its ID.
     * - Returns ProductDto if found; otherwise returns a 404 error with message.
     *
     * @param id the ID of the product to fetch
     * @return ResponseEntity containing ProductDto or error message
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getProduct(@PathVariable Long id) {
        try {
            // Fetch product details using service layer
            ProductDto dto = productService.getProductDtoById(id);
            return ResponseEntity.ok(dto); // Return product details with HTTP 200
        } catch (Exception ex) {
            // Handle product not found or other exceptions
            return ResponseEntity.status(404).body("Product not found");
        }
    }
}
