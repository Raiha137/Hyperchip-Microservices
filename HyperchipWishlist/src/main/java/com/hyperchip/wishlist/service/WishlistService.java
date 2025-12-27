package com.hyperchip.wishlist.service;

import com.hyperchip.wishlist.model.WishlistItem;

import java.util.List;

// -------------------------
// WishlistService
// -------------------------
// Service interface for managing WishlistItems.
// Defines core operations for adding, removing, listing, and saving wishlist items.
// Provides utility for converting image paths to absolute URLs.
//
// Developer Notes:
// - addToWishlist handles max quantity check (3 items per product).
// - removeWishlistItem deletes a specific product from user's wishlist.
// - listByUserId returns all wishlist items for a given user.
// - save is used for updating quantity or saving a new wishlist item.
// - ensureAbsoluteImageUrl converts a relative image path to an absolute URL.

public interface WishlistService {

    // -------------------------
    // Add a product to user's wishlist
    // -------------------------
    WishlistItem addToWishlist(Long userId, Long productId);

    // -------------------------
    // Remove a specific wishlist item for a user
    // -------------------------
    void removeWishlistItem(Long userId, Long productId);

    // -------------------------
    // Retrieve all wishlist items for a given user
    // -------------------------
    List<WishlistItem> listByUserId(Long userId);

    // -------------------------
    // Save or update a wishlist item (e.g., quantity update)
    // -------------------------
    WishlistItem save(WishlistItem item);

    // -------------------------
    // Convert relative image path to absolute URL
    // -------------------------
    String ensureAbsoluteImageUrl(Object imageObj);

}
