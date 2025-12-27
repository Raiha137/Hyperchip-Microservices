package com.hyperchip.cart.controller;

import com.hyperchip.cart.service.CartService;
import com.hyperchip.common.dto.AddToCartRequest;
import com.hyperchip.common.dto.CartResponse;
import com.hyperchip.common.exception.BadRequestException;
import com.hyperchip.common.exception.OutOfStockException;
import com.hyperchip.common.exception.ResourceNotFoundException;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * CartController
 *
 * This controller handles all cart-related APIs
 * such as add item, update quantity, remove items,
 * fetch cart details, and clear cart.
 *
 * All endpoints are user-specific and work
 * based on userId.
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    private static final Logger log = LoggerFactory.getLogger(CartController.class);

    /**
     * Add a product to the cart
     *
     * Validates request data and handles
     * stock, bad request, and not-found cases.
     */
    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@Valid @RequestBody AddToCartRequest request) {
        try {
            CartResponse response = cartService.addToCart(request);
            return ResponseEntity.ok(response);

        } catch (OutOfStockException ex) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("error", ex.getMessage()));

        } catch (BadRequestException ex) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", ex.getMessage()));

        } catch (ResourceNotFoundException ex) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", ex.getMessage()));

        } catch (Exception ex) {
            log.error(
                    "Unexpected error while adding to cart. userId={}, productId={}",
                    request.getUserId(),
                    request.getProductId(),
                    ex
            );
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    /**
     * Get cart details for a user
     */
    @GetMapping("/{userId}")
    public ResponseEntity<CartResponse> getCart(@PathVariable Long userId) {
        return ResponseEntity.ok(cartService.getCartForUser(userId));
    }

    /**
     * Update quantity of a specific product in the cart
     */
    @PutMapping("/{userId}/items/{productId}")
    public ResponseEntity<?> updateQuantity(
            @PathVariable Long userId,
            @PathVariable Long productId,
            @RequestParam Integer qty) {

        try {
            CartResponse response = cartService.updateQuantity(userId, productId, qty);
            return ResponseEntity.ok(response);

        } catch (BadRequestException ex) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", ex.getMessage()));

        } catch (ResourceNotFoundException ex) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", ex.getMessage()));

        } catch (Exception ex) {
            log.error(
                    "Error updating cart quantity. userId={}, productId={}, qty={}",
                    userId,
                    productId,
                    qty,
                    ex
            );
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    /**
     * Remove a single product from the cart
     */
    @DeleteMapping("/{userId}/items/{productId}")
    public ResponseEntity<?> removeItem(
            @PathVariable Long userId,
            @PathVariable Long productId) {

        try {
            cartService.removeItem(userId, productId);
            return ResponseEntity.noContent().build();

        } catch (ResourceNotFoundException ex) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", ex.getMessage()));

        } catch (Exception ex) {
            log.error(
                    "Error removing item from cart. userId={}, productId={}",
                    userId,
                    productId,
                    ex
            );
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    /**
     * Remove multiple products from the cart
     */
    @PostMapping("/{userId}/items/remove")
    public ResponseEntity<CartResponse> removeItems(
            @PathVariable Long userId,
            @RequestBody List<Long> productIds) {

        return ResponseEntity.ok(cartService.removeItems(userId, productIds));
    }

    /**
     * Clear cart completely
     *
     * orderComplete flag is used to
     * differentiate between manual clear
     * and order completion flow.
     */
    @DeleteMapping("/{userId}/clear")
    public ResponseEntity<Void> clearCart(
            @PathVariable Long userId,
            @RequestParam(name = "orderComplete", defaultValue = "false") boolean orderComplete) {

        cartService.clearCart(userId, orderComplete);
        return ResponseEntity.noContent().build();
    }
}
