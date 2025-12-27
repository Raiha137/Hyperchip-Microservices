package com.hyperchip.order.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing an individual item within an Order.
 * Stores a snapshot of product info at the time of order, quantity, pricing, and cancellation info.
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Primary key

    // ---------------- Product Snapshot Fields ----------------
    private Long productId;      // ID of the product at order time
    private String productTitle; // Title/name of the product
    private String productImage; // Main image URL or path

    private Integer quantity; // Quantity of this product in the order
    private Double unitPrice; // Price per unit at the time of order
    private Double total;     // Total = unitPrice * quantity (stored for convenience)

    // ---------------- Cancellation Info ----------------
    @Column(length = 1000)
    private String cancelReason; // Optional reason if this item was cancelled

    @Column(nullable = false, columnDefinition = "bit(1) default 0")
    private Boolean cancelled = Boolean.FALSE; // Flag to indicate if item is cancelled

    public boolean isCancelled() {
        return Boolean.TRUE.equals(this.cancelled);
    }

    // ---------------- Relationship ----------------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order; // Reference to parent order

    // ---------------- Lifecycle Hooks ----------------
    @PrePersist
    void prePersist() {
        if (cancelled == null) cancelled = false; // Ensure default value on insert
    }
}
