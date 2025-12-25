package com.hyperchip.master.controller;

import com.hyperchip.master.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * InventoryController
 *
 * Purpose:
 * - Provides REST APIs to manage product inventory (stock levels) in the system.
 * - Includes incrementing and decrementing product stock.
 */
@RestController
@RequestMapping("/api/master/inventory") // Base path for inventory management
@RequiredArgsConstructor
public class InventoryController {

    private final ProductService productService; // Business logic for product stock management

    // ===========================
    // INCREMENT PRODUCT STOCK
    // ===========================
    /**
     * POST /api/master/inventory/{productId}/increment
     *
     * Purpose:
     * - Increase the stock quantity of a specific product.
     * - Typically used when new stock arrives or an order is cancelled.
     *
     * @param productId ID of the product to increment
     * @param qty       Quantity to add (default = 1 if not specified)
     * @return HTTP 200 OK if successful
     */
    @PostMapping("/{productId}/increment")
    public ResponseEntity<?> increment(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "1") int qty) {

        productService.incrementStock(productId, qty); // Increase product stock
        return ResponseEntity.ok().build();           // Return 200 OK with no body
    }

    // ===========================
    // DECREMENT PRODUCT STOCK
    // ===========================
    /**
     * POST /api/master/inventory/{productId}/decrement
     *
     * Purpose:
     * - Decrease the stock quantity of a specific product.
     * - Typically used when an order is placed or stock is removed.
     *
     * @param productId ID of the product to decrement
     * @param qty       Quantity to subtract (default = 1 if not specified)
     * @return HTTP 200 OK if successful
     */
    @PostMapping("/{productId}/decrement")
    public ResponseEntity<?> decrement(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "1") int qty) {

        productService.decrementStock(productId, qty); // Decrease product stock
        return ResponseEntity.ok().build();           // Return 200 OK with no body
    }
}
