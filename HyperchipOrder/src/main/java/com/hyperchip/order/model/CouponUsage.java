package com.hyperchip.order.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entity representing a record of a coupon being used on a specific order by a user.
 * Tracks which user used which coupon, on which order, and the discount applied.
 */
@Entity
@Table(name = "coupon_usages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Unique ID for this coupon usage record

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coupon_id")
    private Coupon coupon; // Reference to the coupon used

    @Column(name = "order_id", nullable = false)
    private Long orderId; // ID of the order where the coupon was applied

    @Column(name = "user_id", nullable = false)
    private Long userId; // ID of the user who applied the coupon

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount; // Actual discount amount applied on this order

    @Column(name = "used_at", nullable = false)
    private Instant usedAt; // Timestamp when the coupon was used
}
