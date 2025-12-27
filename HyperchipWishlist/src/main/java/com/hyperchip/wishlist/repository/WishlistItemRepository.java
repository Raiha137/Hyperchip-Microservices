package com.hyperchip.wishlist.repository;

import com.hyperchip.wishlist.model.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// -------------------------
// WishlistItemRepository
// -------------------------
// Repository interface for managing WishlistItem entities in the database.
// Provides CRUD operations and custom query methods.
//
// Developer Notes:
// - Uses Spring Data JPA to automatically generate queries based on method names.
// - Custom methods help fetch wishlist items for a specific user, check existence, and retrieve specific product entries.

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    // -------------------------
    // Fetch all wishlist items for a given user
    // -------------------------
    List<WishlistItem> findByUserId(Long userId);

    // -------------------------
    // Fetch a single wishlist item for a user and product
    // -------------------------
    Optional<WishlistItem> findByUserIdAndProductId(Long userId, Long productId);

    // -------------------------
    // Check if a wishlist item exists for a given user and product
    // -------------------------
    boolean existsByUserIdAndProductId(Long userId, Long productId);
}
