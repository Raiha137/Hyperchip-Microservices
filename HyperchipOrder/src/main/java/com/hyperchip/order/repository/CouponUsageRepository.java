package com.hyperchip.order.repository;

import com.hyperchip.order.model.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing CouponUsage records.
 * Tracks when a coupon is applied to an order by a user.
 */
@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {

    /**
     * Check if a coupon usage exists for a given order.
     *
     * @param orderId the order ID
     * @return true if a coupon has been applied to the order
     */
    boolean existsByOrderId(Long orderId);

    /**
     * Find the coupon usage for a specific order.
     *
     * @param orderId the order ID
     * @return an Optional containing the CouponUsage if found
     */
    Optional<CouponUsage> findByOrderId(Long orderId);

    /**
     * Count how many times a particular coupon has been used overall.
     *
     * @param couponId the coupon ID
     * @return total usage count
     */
    int countByCouponId(Long couponId);

    /**
     * Count how many times a particular user has used a specific coupon.
     *
     * @param couponId the coupon ID
     * @param userId the user ID
     * @return usage count for the user
     */
    int countByCouponIdAndUserId(Long couponId, Long userId);

    /**
     * Retrieve all coupon usages for a list of orders.
     * Useful for generating reports or analytics.
     *
     * @param orderIds list of order IDs
     * @return list of CouponUsage records
     */
    List<CouponUsage> findByOrderIdIn(List<Long> orderIds);
}
