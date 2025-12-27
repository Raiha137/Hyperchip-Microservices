package com.hyperchip.cart.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * CartItem Entity
 *
 * This entity represents a single product
 * added to a user's shopping cart.
 *
 * Each cart item is uniquely identified
 * by the combination of cart and product.
 */
@Entity
@Table(
        name = "cart_items",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"cart_id", "product_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    /**
     * Primary key of the cart_items table.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference back to the cart.
     *
     * Many cart items belong to one cart.
     * LAZY fetch is used to avoid
     * unnecessary data loading.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    /**
     * ID of the product added to the cart.
     */
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /**
     * Quantity of the product.
     */
    @Column(nullable = false)
    private Integer quantity;

    /**
     * Price of the product at the time
     * it was added to the cart.
     *
     * This helps in price consistency
     * if product price changes later.
     */
    @Column(name = "unit_price")
    private Double unitPrice;

    /**
     * Timestamp when the product
     * was added to the cart.
     */
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt = LocalDateTime.now();
}
