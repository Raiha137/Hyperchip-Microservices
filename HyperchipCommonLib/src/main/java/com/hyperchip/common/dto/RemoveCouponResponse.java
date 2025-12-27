package com.hyperchip.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * Response returned when a coupon is removed from an order.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemoveCouponResponse {
    private boolean success;
    private String message;
    private Long orderId;
    private BigDecimal totalBeforeDiscount;
    private BigDecimal totalAfterDiscount;
}
