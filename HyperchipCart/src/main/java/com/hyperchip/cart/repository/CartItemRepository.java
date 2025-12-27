package com.hyperchip.cart.repository;

import com.hyperchip.cart.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * CartItemRepository
 *
 * Repository interface for CartItem entity.
 * Provides database operations related
 * to cart items.
 *
 * Spring Data JPA automatically generates
 * the implementation at runtime.
 */
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * Find a cart item by cart ID and product ID.
     *
     * This method is mainly used to:
     * - Check if a product already exists in the cart
     * - Update quantity instead of creating a new entry
     *
     * @param cartId    ID of the cart
     * @param productId ID of the product
     * @return Optional CartItem if found
     */
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);
}
