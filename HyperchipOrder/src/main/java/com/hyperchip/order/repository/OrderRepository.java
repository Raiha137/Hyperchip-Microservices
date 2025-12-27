package com.hyperchip.order.repository;

import com.hyperchip.order.model.Order;
import com.hyperchip.order.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing orders.
 * Provides methods to find orders by user, status, order number, and date range.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Find an order by its unique order number.
     *
     * @param orderNumber the unique order number
     * @return Optional containing the order if found
     */
    Optional<Order> findByOrderNumber(String orderNumber);

    /**
     * Get all orders for a user, latest orders first.
     *
     * @param userId the user ID
     * @return list of orders sorted by creation date descending
     */
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Get paginated orders for a user.
     *
     * @param userId the user ID
     * @param pageable pagination information
     * @return paginated orders
     */
    Page<Order> findByUserId(Long userId, Pageable pageable);

    /**
     * Get paginated orders for a user, latest orders first.
     *
     * @param userId the user ID
     * @param pageable pagination information
     * @return paginated orders sorted by creation date descending
     */
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Find orders by status and partial order number match.
     *
     * @param status order status
     * @param orderNumber partial order number
     * @param pageable pagination information
     * @return paginated orders
     */
    Page<Order> findByStatusAndOrderNumberContainingIgnoreCase(OrderStatus status, String orderNumber, Pageable pageable);

    /**
     * Find orders by partial order number.
     *
     * @param orderNumber partial order number
     * @param pageable pagination information
     * @return paginated orders
     */
    Page<Order> findByOrderNumberContainingIgnoreCase(String orderNumber, Pageable pageable);

    /**
     * Find orders created between two timestamps.
     *
     * @param start start timestamp
     * @param end end timestamp
     * @return list of orders
     */
    List<Order> findByCreatedAtBetween(Instant start, Instant end);
}
