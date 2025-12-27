package com.hyperchip.order.model;

import com.hyperchip.common.dto.DiscountType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entity representing a coupon stored in the Order service database.
 * Stores all details needed to apply a coupon to an order.
 */
@Entity
@Table(name = "coupons")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Unique ID for the coupon

    @Column(nullable = false, unique = true, length = 50)
    private String code; // The code users enter to apply the coupon

    @Column(length = 255)
    private String description; // Text description of the coupon

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType; // Type of discount: PERCENTAGE or FIXED

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue; // Value of the discount (amount or percent)

    @Column(name = "max_discount_amount", precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount; // Maximum discount allowed for percentage type

    @Column(name = "min_order_amount", precision = 10, scale = 2)
    private BigDecimal minOrderAmount; // Minimum order total required to use this coupon

    @Column(name = "start_at")
    private Instant startAt; // Start date/time when coupon becomes valid

    @Column(name = "end_at")
    private Instant endAt; // Expiry date/time of the coupon

    @Column(name = "usage_limit_per_coupon")
    private Integer usageLimitPerCoupon; // Max total uses for this coupon

    @Column(name = "usage_limit_per_user")
    private Integer usageLimitPerUser; // Max uses per individual user

    @Column(name = "active")
    private Boolean active; // Whether the coupon is currently active

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt; // Timestamp when coupon was created

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt; // Timestamp when coupon was last updated
}
