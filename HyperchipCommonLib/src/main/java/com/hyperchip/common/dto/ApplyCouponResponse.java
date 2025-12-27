package com.hyperchip.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response returned when a coupon is applied to an order.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyCouponResponse {
    private boolean success;
    private String message;
    private Long orderId;
    private String couponCode;
    private BigDecimal discountAmount;
    private BigDecimal totalBeforeDiscount;
    private BigDecimal totalAfterDiscount;
    private LocalDateTime appliedAt;
}
