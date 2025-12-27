package com.hyperchip.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request object for removing a coupon from an order.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemoveCouponRequest {
    private Long orderId;
    private Long userId;
}
