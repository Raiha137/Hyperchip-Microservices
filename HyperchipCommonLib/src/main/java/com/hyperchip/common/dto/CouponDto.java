package com.hyperchip.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data transfer object representing a coupon.
 *
 * <p>This class lives in the common module so that it can be shared between
 * services without introducing a direct dependency on JPA or any other
 * persistence framework. It mirrors the fields we need to expose to both
 * the user-facing and admin-facing APIs for coupon management.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponDto {
    private Long id;
    private String code;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderAmount;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer usageLimitPerCoupon;
    private Integer usageLimitPerUser;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
