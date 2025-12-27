// -------------------------
// WishlistItem Entity
// -------------------------
// Represents a user's wishlist item in the Hyperchip e-commerce platform.
// Each record links a user to a product with a specific quantity.
//
// Developer Notes:
// - `userId` + `productId` is unique to avoid duplicate wishlist entries.
// - `quantity` defaults to 1.
// - `createdAt` automatically stores the timestamp when the entry is created.
package com.hyperchip.wishlist.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "wishlist_items", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "product_id"}, name = "uq_wishlist_user_product")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistItem {

    // -------------------------
    // Primary Key
    // -------------------------
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // -------------------------
    // User ID
    // -------------------------
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // -------------------------
    // Product ID
    // -------------------------
    @Column(name = "product_id", nullable = false)
    private Long productId;

    // -------------------------
    // Timestamp when the item is created
    // -------------------------
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // -------------------------
    // Quantity of the product in wishlist (1..3)
    // -------------------------
    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;
}
