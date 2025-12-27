package com.hyperchip.cart.repository;

import com.hyperchip.cart.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * CartRepository
 *
 * Repository interface for Cart entity.
 * Provides database operations related to user carts.
 *
 * Spring Data JPA automatically implements this interface at runtime.
 */
public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * Find a cart by the user ID.
     *
     * This is mainly used to fetch the active cart
     * for a given user.
     *
     * @param userId ID of the user
     * @return Optional containing Cart if found
     */
    Optional<Cart> findByUserId(Long userId);

    /**
     * Delete a cart by the user ID.
     *
     * Useful when clearing the cart after order completion
     * or when manually removing all items for a user.
     *
     * @param userId ID of the user
     */
    void deleteByUserId(Long userId);
}
