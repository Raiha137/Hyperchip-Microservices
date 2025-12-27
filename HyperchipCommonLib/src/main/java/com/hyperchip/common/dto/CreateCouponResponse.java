package com.hyperchip.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Response object returned after creating a new coupon via the admin API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCouponResponse {
    private Long id;
    private String code;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
