package com.hyperchip.order.repository;

import com.hyperchip.order.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing order items.
 * Each OrderItem represents a single product in an order.
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Find a specific order item by its ID and the associated order ID.
     * Useful for validating that an item belongs to a specific order.
     *
     * @param id the ID of the order item
     * @param orderId the ID of the order
     * @return Optional containing the OrderItem if found, empty otherwise
     */
    Optional<OrderItem> findByIdAndOrderId(Long id, Long orderId);
}
