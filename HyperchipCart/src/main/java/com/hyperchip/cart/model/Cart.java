package com.hyperchip.cart.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Cart Entity
 *
 * This entity represents a user's shopping cart.
 * One cart belongs to one user and contains
 * multiple cart items.
 */
@Entity
@Table(name = "carts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cart {

    /**
     * Primary key of the cart table.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID of the user who owns this cart.
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * List of items present in the cart.
     *
     * - One cart can have multiple cart items
     * - Cascade ALL ensures items are saved/removed with the cart
     * - orphanRemoval removes unused cart items automatically
     */
    @OneToMany(
            mappedBy = "cart",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private List<CartItem> items = new ArrayList<>();

    /**
     * Time when the cart was created.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Time when the cart was last updated.
     */
    private LocalDateTime updatedAt;

    /**
     * Helper method to add an item to the cart.
     * Maintains the bidirectional relationship.
     */
    public void addItem(CartItem item) {
        if (items == null) {
            items = new ArrayList<>();
        }
        item.setCart(this);
        items.add(item);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Helper method to remove an item from the cart.
     * Ensures proper cleanup of relationships.
     */
    public void removeItem(CartItem item) {
        if (items != null) {
            items.remove(item);
            item.setCart(null);
            this.updatedAt = LocalDateTime.now();
        }
    }
}
