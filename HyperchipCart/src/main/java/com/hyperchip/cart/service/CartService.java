// =========================
// CartService.java
// =========================
package com.hyperchip.cart.service;

import com.hyperchip.common.dto.AddToCartRequest;
import com.hyperchip.common.dto.CartResponse;

import java.util.List;

/**
 * CartService
 *
 * Main intention:
 * This interface defines the contract for cart-related operations.
 * All cart-related actions (add, update, remove, fetch, clear) are declared here.
 * Implementation classes (like CartServiceImpl) should handle actual logic, stock management, and DTO mapping.
 */
public interface CartService {

    /**
     * Fetch the current cart for a specific user.
     *
     * @param userId the ID of the user
     * @return CartResponse containing cart items, subtotal, and total items
     */
    CartResponse getCartForUser(Long userId);

    /**
     * Add a product to the user's cart.
     * If the product already exists in the cart, it increments the quantity.
     *
     * @param req DTO containing productId, userId, and optional quantity
     * @return updated CartResponse
     */
    CartResponse addToCart(AddToCartRequest req);

    /**
     * Update the quantity of a specific product in the user's cart.
     * Quantity 0 removes the product.
     *
     * @param userId    the ID of the user
     * @param productId the ID of the product
     * @param quantity  the new quantity
     * @return updated CartResponse
     */
    CartResponse updateQuantity(Long userId, Long productId, Integer quantity);

    /**
     * Remove a single item from the user's cart.
     *
     * @param userId    the ID of the user
     * @param productId the ID of the product to remove
     */
    void removeItem(Long userId, Long productId);

    /**
     * Remove multiple items from the user's cart at once.
     *
     * @param userId     the ID of the user
     * @param productIds list of product IDs to remove
     * @return updated CartResponse
     */
    CartResponse removeItems(Long userId, List<Long> productIds);

    /**
     * Clear all items from the user's cart.
     * Older callers can use this method (default behavior: orderComplete = false).
     *
     * @param userId the ID of the user
     */
    void clearCart(Long userId);

    /**
     * Clear all items from the user's cart with an option to indicate
     * whether the cart is being cleared due to order completion.
     * If orderComplete = true, stock should not be restored.
     *
     * @param userId        the ID of the user
     * @param orderComplete true if this clear is due to order completion
     */
    void clearCart(Long userId, boolean orderComplete);

}
